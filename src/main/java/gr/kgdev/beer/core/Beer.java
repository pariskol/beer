package gr.kgdev.beer.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gr.kgdev.beer.model.SimpleErrorMessage;
import gr.kgdev.beer.model.SimpleMessage;
import gr.kgdev.beer.model.exceptions.BadRequestException;
import gr.kgdev.beer.model.exceptions.ForbiddenException;
import gr.kgdev.beer.model.exceptions.UnauthorizedException;
import gr.kgdev.beer.utils.BeerUtils;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class Beer {

	public static final String PATH_PREFIX = "path:";
	private ServletContextHandler context;
	private Server server;
	private final Map<String, Map<String, RequestHandler>> routes = new HashMap<>();
	private final Map<String, Map<String, RequestHandler>> wildcardRoutes = new HashMap<>();
	private final Map<String, Map<String, RequestHandler>> pathParamRoutes = new HashMap<>();
	private final HandlerList handlers = new HandlerList();
	private BeerConfig config;
	private String staticFilePath;
	private Logger logger = LoggerFactory.getLogger(Beer.class);

	public void init(BeerConfig config) {
		var threadPool = new QueuedThreadPool(config.getJettyMaxThreads(), config.getJettyMinThreads(),
				config.getJettyIdleTimeout());

		this.config = config;
		this.server = new Server(threadPool);

		ServerConnector connector;
		
		if (config.getLoggerName() != null) {
			logger = LoggerFactory.getLogger(config.getLoggerName());
		}
		
		// if keystore is set, only https will be available
		if (config.getKeystorePath() != null) {
	        var sslContextFactory = new SslContextFactory.Server();
	        sslContextFactory.setKeyStorePath(config.getKeystorePath());
	        sslContextFactory.setKeyStorePassword(config.getKeystorePass());
	        sslContextFactory.setKeyManagerPassword(config.getKeystorePass());
	        
			connector = new ServerConnector(
					server,
					new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
		            new HttpConnectionFactory()
	        );
			
			server.addConnector(connector);
		}
		else {
			connector = new ServerConnector(server);
		}
        
		connector.setHost(config.getIp());
		connector.setPort(config.getPort());

		server.addConnector(connector);
		this.context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");

		handlers.addHandler(context);
	}

	public Server getServerInstance() {
		return server;
	}

	private HttpServlet createServlet() {
		@SuppressWarnings("serial")
		var servlet = new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse res)
					throws ServletException, IOException {
				var httpMethod = req.getMethod().toUpperCase();
				res.setContentType("application/json");
	
				var reqURI = req.getRequestURI();
	
				// 1. Try exact match
				var methodHandlers = routes.get(reqURI);
	
				// 2. If no exact match, try wildcard match
				if (methodHandlers == null) {
					for (var entry : wildcardRoutes.entrySet()) {
						var wildcardPath = entry.getKey().substring(0, entry.getKey().length() - 1);
						if (reqURI.startsWith(wildcardPath)) {
							methodHandlers = entry.getValue();
							break;
						}
					}
				}
	
				// 3. If no wildcard match, try param match
				if (methodHandlers == null) {
				    for (var entry : pathParamRoutes.entrySet()) {
				        String pattern = entry.getKey(); // e.g. "/api/files/:fileid"
				        if (matchesParamPattern(pattern, reqURI)) {
				        	setAttributesForPathParams(pattern, req);
				            methodHandlers = entry.getValue();
				            break;
				        }
				    }
				}
				if (methodHandlers != null) {
					var registeredHandler = methodHandlers.get(httpMethod);
					if (registeredHandler != null) {
						try {
							var result = registeredHandler.handle(req, res);
							res.setStatus(200);
							res.getWriter().write(BeerUtils.json(result));
							return;
						} catch (Throwable e) {
							throw new ServletException(e);
						}
					}
					
					res.setStatus(405);
					res.getWriter().write(BeerUtils.json(new SimpleMessage("Method Not Allowed")));
					return;
				}
	
				res.setStatus(404);
				res.getWriter().write(BeerUtils.json(new SimpleMessage("Not Found")));
			}
		};
		return servlet;
	}

	private void setAttributesForPathParams(String pattern, HttpServletRequest req) {
		var patternParts = pattern.split("/");
		var uriParts = req.getRequestURI().split("/");
		
	    if (patternParts.length != uriParts.length) {
	        // do not extract if mismatch
	        return;
	    }
	    
		for (var i = 0; i < patternParts.length; i++) {
			if (patternParts[i].startsWith(":")) {
				String paramName = patternParts[i].substring(1);
				// store path param value in req
				req.setAttribute(PATH_PREFIX + paramName, uriParts[i]);
			}
		}
	}
	
	private boolean matchesParamPattern(String pattern, String uri) {
		var patternParts = pattern.split("/");
		var uriParts = uri.split("/");

		if (patternParts.length != uriParts.length) {
			return false;
		}

		for (var i = 0; i < patternParts.length; i++) {
			if (patternParts[i].startsWith(":")) {
				continue; // matches anything
			}
			if (!patternParts[i].equals(uriParts[i])) {
				return false;
			}
		}

		return true;
	}

	private void add(String method, String path, RequestHandler handler) {
	    method = method.toUpperCase();

	    if (staticFilePath.equals(path)) {
	        throw new IllegalStateException("Path '" + path + "' is already occupied for serving files");
	    }

	    boolean isWildcard = path.endsWith("*");
	    boolean isParam = path.contains(":");

	    Map<String, Map<String, RequestHandler>> targetRoutes;

	    if (isWildcard) {
	        targetRoutes = wildcardRoutes;
	    } else if (isParam) {
	        targetRoutes = pathParamRoutes;
	    } else {
	        targetRoutes = routes;
	    }

	    targetRoutes.computeIfAbsent(path, newPath -> {
	        var servlet = createServlet();
	        context.addServlet(new ServletHolder(servlet), pathToServletMapping(newPath));
	        return new HashMap<>();
	    }).put(method, handler);
	}

	private String pathToServletMapping(String path) {
	    if (path.contains(":")) {
	        // Param routes need to be mapped to a wildcard servlet mapping.
	        // Jetty requires something like "/api/files/*" instead of "/api/files/:fileid"
	        // So, replace ":param" parts with "*" or just end with "/*"
	        // Example: "/api/files/:fileid" => "/api/files/*"
	        int idx = path.indexOf("/:"); 
	        if (idx >= 0) {
	            return path.substring(0, idx) + "/*";
	        }
	        return path; // fallback
	    }
	    return path;
	}


	public void get(String path, RequestHandler handler) {
		add("GET", path, handler);
	}

	public void post(String path, RequestHandler handler) {
		add("POST", path, handler);
	}

	public void put(String path, RequestHandler handler) {
		add("PUT", path, handler);
	}

	public void delete(String path, RequestHandler handler) {
		add("DELETE", path, handler);
	}

	public void filter(String path, FilterHandler handler) {
		Filter servletFilter = new Filter() {
			@Override
			public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
					throws ServletException, IOException {
				try {
					handler.handle((HttpServletRequest) req, (HttpServletResponse) res);
					chain.doFilter(req, res);
				} catch (Exception ex) {
					throw new ServletException(ex);
				}
			}

			@Override
			public void init(FilterConfig filterConfig) {
			}

			@Override
			public void destroy() {
			}
		};

		context.addFilter(new FilterHolder(servletFilter), path, null);
	}

	public void exceptionFilter() {
		Filter servletFilter = new Filter() {
			@Override
			public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
					throws ServletException, IOException {
				try {
					chain.doFilter(req, res);
				} catch (ServletException ex) {
					handleException((HttpServletRequest) req, (HttpServletResponse) res, ex.getRootCause());
				} catch (Throwable ex) {
					handleException((HttpServletRequest) req, (HttpServletResponse) res, ex);
				}
			}

			@Override
			public void init(FilterConfig filterConfig) {
			}

			@Override
			public void destroy() {
			}
		};

		context.addFilter(new FilterHolder(servletFilter), "/*", null);
	}

	private void handleException(HttpServletRequest req, HttpServletResponse res, Throwable ex) throws IOException {
		res.setContentType("application/json");
		var tag = UUID.randomUUID().toString();

		if (ex instanceof ForbiddenException) {
			res.setStatus(403);
			res.getWriter().write(BeerUtils.json(new SimpleErrorMessage("Forbidden", tag)));
		} else if (ex instanceof UnauthorizedException) {
			res.setStatus(401);
			res.getWriter().write(BeerUtils.json(new SimpleErrorMessage("Unauthorized", tag)));
		} else if (ex instanceof BadRequestException) {
			res.setStatus(400);
			res.getWriter().write(BeerUtils.json(new SimpleErrorMessage(ex.getMessage(), tag)));

		} else {
			res.setStatus(500);
			res.getWriter().write(BeerUtils.json(new SimpleErrorMessage("Ops something went wrong!", tag)));
		}
		
		if (res.getStatus() == 500) {
			logger.error(req.getMethod() + " " + req.getRequestURI(), ex);
		} else {
			logger.error(req.getMethod() + " " + req.getRequestURI() + " " + ex.getMessage());
		}
	}

	public void staticFiles(String path, String filePathInClasspath) {
		if (routes.containsKey(path)) {
			throw new IllegalStateException("Path provided to map files is alreday occupied by another route");
		}
		
		routes.put(path, null);
		
		context.setBaseResource(Resource.newClassPathResource(filePathInClasspath));
		context.addServlet(new ServletHolder(new DefaultServlet()), path);
		context.setWelcomeFiles(new String[]{"index.html"});
		
		staticFilePath = path;
	}

	public void corsAllFilter() {
		Filter servletFilter = new Filter() {
			@Override
			public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
					throws ServletException, IOException {
				try {
					var httpRes = (HttpServletResponse) res;
					httpRes.setHeader("Access-Control-Allow-Origin", "*");
					httpRes.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
					httpRes.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
					
					var httpReq = (HttpServletRequest) req;
					if (httpReq.getMethod().equalsIgnoreCase("options")) {
						httpRes.setStatus(200);
						return;
					}
					
					chain.doFilter(req, res);
				} catch (Exception ex) {
					throw new ServletException(ex);
				}
			}

			@Override
			public void init(FilterConfig filterConfig) {
			}

			@Override
			public void destroy() {
			}
		};

		context.addFilter(new FilterHolder(servletFilter), "/*", null);
	}

	public void loggingFilter() {
		filter("/*", (req, res) -> {
			logger.info("[LOG] " + req.getMethod() + " " + req.getRequestURI());
		});
	}

	public void start() throws Exception {
		if (config == null) {
			throw new IllegalStateException("Beer is not configured! Call init() method first!");
		}
		
		server.setHandler(handlers);
		server.start();
		logger.info("Server started on http://" + config.getIp() + ":" + config.getPort());
		server.join();
	}

}
