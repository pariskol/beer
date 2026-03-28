package gr.kgdev.beer.core;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jetty.ee11.servlet.DefaultServlet;
import org.eclipse.jetty.ee11.servlet.FilterHolder;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.ee11.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.ee11.websocket.server.JettyWebSocketServletFactory;
import org.eclipse.jetty.ee11.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.api.Session;
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


/**
 * Root entry point and core engine of the Beer web framework.
 * <p>
 * {@code Beer} is a lightweight HTTP and WebSocket server built on top of Jetty
 * using servlets, providing a minimal yet expressive API for defining routes,
 * middleware filters, static file serving, and real-time WebSocket endpoints.
 *
 * <p>
 * Internally, {@code Beer} dynamically creates and registers Jetty servlets
 * per route group and resolves requests in a deterministic order:
 * exact routes, wildcard routes, and parameterized routes.
 */
public class Beer {

	public static final String PATH_PREFIX = "path:";
	private ServletContextHandler context;
	private Server server;
	private final Map<String, Map<String, RequestHandler>> routes = new HashMap<>();
	private final Map<String, Map<String, RequestHandler>> wildcardRoutes = new HashMap<>();
	private final Map<String, Map<String, RequestHandler>> pathParamRoutes = new HashMap<>();
	private final Map<String, List<Session>> socketSessionsRoutesMap = new ConcurrentHashMap<>();
	private final Handler.Sequence handlers = new Handler.Sequence();
	private BeerConfig config;
	private String staticFilePath = "";
	private Logger logger = LoggerFactory.getLogger(Beer.class);

	/**
	 * Initializes the Beer server with the provided configuration.
	 * Sets up thread pool, connectors, context, and handlers.
	 *
	 * @param config the BeerConfig instance containing server settings
	 */
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
		
		 JettyWebSocketServletContainerInitializer.configure(context, null);

		handlers.addHandler(context);
	}

	/**
     * Registers a WebSocket endpoint at the given path.
     *
     * @param path the WebSocket route path
     */
	@SuppressWarnings("unused")
	public void socket(String path) {
		socket(path, (msg) -> {});
	}
	

	/**
     * Registers a WebSocket endpoint at the given path with a message handler.
     *
     * @param path the WebSocket route path
     * @param onMessage consumer for incoming messages
     */
	@SuppressWarnings("serial")
	public void socket(String path, Consumer<Object> onMessage) {
		var websocketServlet = new JettyWebSocketServlet() {
			@SuppressWarnings("unused")
			@Override
			protected void configure(JettyWebSocketServletFactory factory) {
			    factory.setIdleTimeout(Duration.ZERO); // No timeout
			    factory.setMaxTextMessageSize(1048576); // 1 mb
				factory.addMapping(path,
						(req, res) -> new BeerSocket(
								session -> {
									if (socketSessionsRoutesMap.get(path) == null) {
										socketSessionsRoutesMap.put(path, new CopyOnWriteArrayList<>());
									}
									socketSessionsRoutesMap.get(path).add(session);
								},
								(session, msg) -> onMessage.accept(msg),
								session -> {
									socketSessionsRoutesMap.get(path).remove(session);
								}));
			}
		};
		context.addServlet(new ServletHolder(websocketServlet), path);
	}


	/**
     * Returns the underlying Jetty Server instance.
     *
     * @return the Jetty Server
     */
	public Server getServerInstance() {
		return server;
	}

	/**
	 * Creates and returns a dynamically configured {@link HttpServlet}
	 * responsible for routing incoming HTTP requests to registered handlers.
	 * <p>
	 * The servlet resolves requests using the following matching strategy
	 * (in order of precedence):
	 * <ol>
	 *   <li><b>Exact path match</b> (e.g. {@code /api/users})</li>
	 *   <li><b>Wildcard match</b> (e.g. {@code /api/files/*})</li>
	 *   <li><b>Path parameter match</b> (e.g. {@code /api/files/:fileId})</li>
	 * </ol>
	 *
	 * <p>
	 * Once a route is matched, the HTTP method (GET, POST, etc.) is used to
	 * locate the appropriate {@link RequestHandler}. If found, the handler
	 * is executed and its return value is written as a JSON response.
	 *
	 * <p>
	 * Response behavior:
	 * <ul>
	 *   <li>{@code 200 OK} – handler executed successfully</li>
	 *   <li>{@code 404 Not Found} – no matching route</li>
	 *   <li>{@code 405 Method Not Allowed} – route exists but method is unsupported</li>
	 * </ul>
	 *
	 * @return a fully configured {@link HttpServlet} instance
	 */
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
							if (result != null) {
								var payload = result instanceof String ? (String) result : BeerUtils.json(result);
								res.getWriter().write(payload);
							}
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

	/**
	 * Extracts path parameter values from the request URI and stores them
	 * as request attributes.
	 * <p>
	 * Path parameters are identified by segments in the route pattern
	 * prefixed with {@code ':'}. Each extracted value is stored in the
	 * request using a predefined prefix.
	 *
	 * <p>
	 * Example:
	 * <pre>{@code
	 * Pattern: /api/files/:fileId
	 * URI:     /api/files/123
	 * Result:  request attribute "path.fileId" = "123"
	 * }</pre>
	 *
	 * <p>
	 * If the pattern and request URI segment counts do not match,
	 * no attributes are extracted.
	 *
	 * @param pattern the registered route pattern containing path parameters
	 * @param req the {@link HttpServletRequest} to store extracted values in
	 */
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
	
	/**
	 * Determines whether a request URI matches a parameterized route pattern.
	 * <p>
	 * Static path segments must match exactly, while segments prefixed
	 * with {@code ':'} are treated as wildcards that match any value.
	 *
	 * <p>
	 * Example:
	 * <pre>{@code
	 * Pattern: /api/files/:fileId
	 * URI:     /api/files/123     -> true
	 * URI:     /api/files/abc     -> true
	 * URI:     /api/users/123     -> false
	 * }</pre>
	 *
	 * @param pattern the route pattern containing optional path parameters
	 * @param uri the incoming request URI
	 * @return {@code true} if the URI matches the pattern; {@code false} otherwise
	 */
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

	/**
	 * Registers a request handler for a given HTTP method and path.
	 * <p>
	 * The path is categorized into one of three routing types:
	 * <ul>
	 *   <li><b>Exact routes</b> – static paths with no parameters</li>
	 *   <li><b>Wildcard routes</b> – paths ending with {@code *}</li>
	 *   <li><b>Parameterized routes</b> – paths containing {@code :param}</li>
	 * </ul>
	 *
	 * <p>
	 * When a new path is encountered, a servlet is created and registered
	 * with the servlet context using a servlet-compatible mapping.
	 *
	 * <p>
	 * Attempting to register a handler on a path reserved for static
	 * file serving will result in an exception.
	 *
	 * @param method the HTTP method (GET, POST, PUT, DELETE, etc.)
	 * @param path the route path
	 * @param handler the {@link RequestHandler} responsible for processing requests
	 * @throws IllegalStateException if the path conflicts with static file handling
	 */
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

	/**
	 * Converts a route-style path definition into a servlet-compatible mapping.
	 * <p>
	 * This method is primarily used to adapt parameterized routes (e.g. {@code :id})
	 * into wildcard servlet mappings required by Jetty.
	 * <p>
	 * Jetty does not support named path parameters such as {@code :fileId} directly
	 * in servlet mappings. Instead, routes containing parameters must be registered
	 * using a wildcard suffix ({@code /*}).
	 * <p>
	 * Example:
	 * <pre>{@code
	 * "/api/files/:fileId" -> "/api/files/*"
	 * }</pre>
	 *
	 * @param path the original route path, possibly containing parameter segments
	 *             prefixed with {@code ':'}
	 * @return a servlet-compatible mapping string suitable for Jetty
	 */
	private String pathToServletMapping(String path) {
	    if (path.contains(":")) {
	        int idx = path.indexOf("/:"); 
	        if (idx >= 0) {
	            return path.substring(0, idx) + "/*";
	        }
	        return path; // fallback
	    }
	    return path;
	}

	/**
     * Registers a GET route handler.
     *
     * @param path the route path
     * @param handler the request handler
     */
	public void get(String path, RequestHandler handler) {
		add("GET", path, handler);
	}

	/**
     * Registers a POST route handler.
     *
     * @param path the route path
     * @param handler the request handler
     */
	public void post(String path, RequestHandler handler) {
		add("POST", path, handler);
	}

	/**
     * Registers a PUT route handler.
     *
     * @param path the route path
     * @param handler the request handler
     */
	public void put(String path, RequestHandler handler) {
		add("PUT", path, handler);
	}

	/**
     * Registers a DELETE route handler.
     *
     * @param path the route path
     * @param handler the request handler
     */
	public void delete(String path, RequestHandler handler) {
		add("DELETE", path, handler);
	}

	 /**
     * Registers a filter that runs before route for the specified path.
     *
     * @param path the filter path
     * @param handler the filter handler
     */
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

	/**
	 * Registers a global exception filter to handle uncaught exceptions.
	 * <p>
	 * Handles internal exception classes and maps them to HTTP status codes:
	 * <ul>
	 *   <li>{@code BadRequestException} (400)</li>
	 *   <li>{@code ForbiddenException} (403)</li>
	 *   <li>{@code UnauthorizedException} (401)</li>
	 *   <li>{@code InternalServerErrorException} (500)</li>
	 *   <li>Other exceptions (500)</li>
	 * </ul>
	 * Always returns a JSON response of {@link gr.kgdev.beer.model.SimpleErrorMessage}.
	 */
	public void exceptionFilter() {
		Filter servletFilter = new Filter() {
			@Override
			public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
					throws ServletException, IOException {
				try {
					chain.doFilter(req, res);
				} catch (ServletException ex) {
					handleException((HttpServletRequest) req, (HttpServletResponse) res, ExceptionUtils.getRootCause(ex));
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
		var datetime = LocalDateTime.now().toString();

		if (ex instanceof ForbiddenException) {
			res.setStatus(403);
			res.getWriter().write(BeerUtils.json(new SimpleErrorMessage("Forbidden", tag, datetime)));
		} else if (ex instanceof UnauthorizedException) {
			res.setStatus(401);
			res.getWriter().write(BeerUtils.json(new SimpleErrorMessage("Unauthorized", tag, datetime)));
		} else if (ex instanceof BadRequestException) {
			res.setStatus(400);
			res.getWriter().write(BeerUtils.json(new SimpleErrorMessage(ex.getMessage(), tag, datetime)));

		} else {
			res.setStatus(500);
			res.getWriter().write(BeerUtils.json(new SimpleErrorMessage("Ops something went wrong!", tag, datetime)));
		}
		
		if (res.getStatus() == 500) {
			logger.error(req.getMethod() + " " + tag + " " + req.getRequestURI(), ex);
		} else {
			logger.error(req.getMethod() + " " + tag + " " + req.getRequestURI() + " " + ex.getMessage());
		}
	}

	/**
     * Serves static files from the specified classpath location (within the jar) at the given path.
     *
     * @param path the route path for static files
     * @param filePathInClasspath the classpath location of static files
     */
	public void staticFiles(String path, String filePathInClasspath) {
		if (routes.containsKey(path)) {
			throw new IllegalStateException("Path provided to map files is alreday occupied by another route");
		}
		
		routes.put(path, null);
		

		// Assuming filePathInClasspath points to a folder inside your JAR, e.g., "static"
		var resourceUrl = ClassLoader.getSystemClassLoader().getResource(filePathInClasspath);
		if (resourceUrl == null) {
		    throw new IllegalArgumentException("Classpath resource not found: " + filePathInClasspath);
		}

		var resourceFactory = ResourceFactory.of(context);

		context.setBaseResource(
		        resourceFactory.newClassLoaderResource(filePathInClasspath)
		);
		context.addServlet(new ServletHolder(new DefaultServlet()), path);
		context.setWelcomeFiles(new String[]{"index.html"});
		
		staticFilePath = path;
	}

	/**
     * Registers a CORS filter that allows all origins and common HTTP methods.
     */
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

	 /**
     * Registers default logging filter for all requests.
     */
	@SuppressWarnings("unused")
	public void loggingFilter() {
		filter("/*", (req, res) -> {
			logger.info("[LOG] " + req.getMethod() + " " + req.getRequestURI());
		});
	}

	/**
     * Starts the Beer server. Blocks until the server is stopped.
     *
     * @throws Exception if the server fails to start
     */
	public void start() throws Exception {
		if (config == null) {
			throw new IllegalStateException("Beer is not configured! Call init() method first!");
		}
		
		server.setHandler(handlers);
		server.start();
		logger.info("Server started on http://" + config.getIp() + ":" + config.getPort());
		server.join();
	}

	/**
     * Returns the map of WebSocket sessions for each route.
     *
     * @return map of route paths to session lists
     */
	public Map<String, List<Session>> getSocketSessionsRoutesMap() {
        return socketSessionsRoutesMap;
    }

	 /**
     * Broadcasts a message to all WebSocket sessions registered at the given path.
     *
     * @param path the WebSocket route path
     * @param data the message data to broadcast
     */
	public void broadcast(String path, Object data) {
		var sessions = socketSessionsRoutesMap.get(path);
		if (sessions != null) {
			sessions.removeIf(session -> session == null || !session.isOpen());
			for (Session session : sessions) {
				try {
					session.sendText(BeerUtils.json(data), null);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

}
