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

	private ServletContextHandler context;
	private Server server;
	private final Map<String, Map<String, RequestHandler>> routes = new HashMap<>();
	private final Map<String, Map<String, RequestHandler>> wildcardRoutes = new HashMap<>();
	private HandlerList handlers = new HandlerList();
	private BeerConfig config;
	private String staticFilePath;

	public void init(BeerConfig config) {
		var threadPool = new QueuedThreadPool(config.getJettyMaxThreads(), config.getJettyMinThreads(),
				config.getJettyIdleTimeout());

		this.config = config;
		this.server = new Server(threadPool);

		ServerConnector connector;
		
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

	private void add(String method, String path, RequestHandler handler) {
		method = method.toUpperCase();

		if (staticFilePath.equals(path)) {
			throw new IllegalStateException("Path '" + path + "' is already occupied for serving files");
		}

		boolean isWildcard = path.endsWith("*");

		Map<String, Map<String, RequestHandler>> targetRoutes = isWildcard ? wildcardRoutes : routes;

		targetRoutes.computeIfAbsent(path, newPath -> {
			@SuppressWarnings("serial")
			var servlet = new HttpServlet() {
				@Override
				protected void service(HttpServletRequest req, HttpServletResponse res)
						throws ServletException, IOException {
					var httpMethod = req.getMethod().toUpperCase();
					res.setContentType("application/json");

					String requestURI = req.getRequestURI();
					System.out.println(requestURI);

					// 1. Try exact match
					var methodHandlers = routes.get(requestURI);

					// 2. If no exact match, try wildcard match
					if (methodHandlers == null) {
						for (var entry : wildcardRoutes.entrySet()) {
							String wildcardPath = entry.getKey().substring(0, entry.getKey().length() - 1);
							if (requestURI.startsWith(wildcardPath)) {
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

			context.addServlet(new ServletHolder(servlet), path);
			return new HashMap<>();
		}).put(method, handler);
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
			LoggerFactory.getLogger("spark").error(req.getMethod() + " " + req.getRequestURI(), ex);
		} else {
			LoggerFactory.getLogger("spark").error(req.getMethod() + " " + req.getRequestURI() + " " + ex.getMessage());
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
		filter("/*", (req, res) -> {
			res.setHeader("Access-Control-Allow-Origin", "*");
			res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
			res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
			res.setStatus(200);
		});
	}

	public void loggingFilter() {
		filter("/*", (req, res) -> {
			LoggerFactory.getLogger("spark").info("[LOG] " + req.getMethod() + " " + req.getRequestURI());
		});
	}

	public void start() throws Exception {
		if (config == null) {
			throw new IllegalStateException("Beer is not configured! Call init() method first!");
		}
		
		server.setHandler(handlers);
		server.start();
		LoggerFactory.getLogger("spark").info("Server started on http://" + config.getIp() + ":" + config.getPort());
		server.join();
	}

}
