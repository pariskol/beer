
package gr.kgdev.beer.core;

/**
 * Configuration class for the Beer server.
 * <p>
 * Holds server settings such as IP, port, Jetty thread pool parameters, SSL
 * keystore, and logger name.
 */
public class BeerConfig {

	/** The IP address the server will bind to. */
	private String ip;

	/** The port number the server will listen on. */
	private Integer port;

	/** The minimum number of threads for the Jetty thread pool. */
	private Integer jettyMinThreads;

	/** The maximum number of threads for the Jetty thread pool. */
	private Integer jettyMaxThreads;

	/** The idle timeout (in milliseconds) for Jetty threads. */
	private Integer jettyIdleTimeout;

	/** The path to the SSL keystore file (enables HTTPS if set). */
	private String keystorePath;

	/** The password for the SSL keystore. */
	private String keystorePass;

	/**
	 * The name of the logger to use.
	 * <p>
	 * SLF4J is used for logging.
	 */
	private String loggerName;

	/**
	 * Gets the IP address the server will bind to.
	 * 
	 * @return the IP address
	 */
	public String getIp() {
		return ip;
	}

	/**
	 * Sets the IP address the server will bind to.
	 * 
	 * @param ip the IP address
	 * @return this config instance
	 */
	public BeerConfig setIp(String ip) {
		this.ip = ip;
		return this;
	}

	/**
	 * Gets the port number the server will listen on.
	 * 
	 * @return the port number
	 */
	public Integer getPort() {
		return port;
	}

	/**
	 * Sets the port number the server will listen on.
	 * 
	 * @param port the port number
	 * @return this config instance
	 */
	public BeerConfig setPort(Integer port) {
		this.port = port;
		return this;
	}

	/**
	 * Gets the minimum number of threads for the Jetty thread pool.
	 * 
	 * @return the minimum thread count
	 */
	public Integer getJettyMinThreads() {
		return jettyMinThreads;
	}

	/**
	 * Sets the minimum number of threads for the Jetty thread pool.
	 * 
	 * @param jettyMinThreads the minimum thread count
	 * @return this config instance
	 */
	public BeerConfig setJettyMinThreads(Integer jettyMinThreads) {
		this.jettyMinThreads = jettyMinThreads;
		return this;
	}

	/**
	 * Gets the maximum number of threads for the Jetty thread pool.
	 * 
	 * @return the maximum thread count
	 */
	public Integer getJettyMaxThreads() {
		return jettyMaxThreads;
	}

	/**
	 * Sets the maximum number of threads for the Jetty thread pool.
	 * 
	 * @param jettyMaxThreads the maximum thread count
	 * @return this config instance
	 */
	public BeerConfig setJettyMaxThreads(Integer jettyMaxThreads) {
		this.jettyMaxThreads = jettyMaxThreads;
		return this;
	}

	/**
	 * Gets the idle timeout (in milliseconds) for Jetty threads.
	 * 
	 * @return the idle timeout in ms
	 */
	public Integer getJettyIdleTimeout() {
		return jettyIdleTimeout;
	}

	/**
	 * Sets the idle timeout (in milliseconds) for Jetty threads.
	 * 
	 * @param jettyIdleTimeout the idle timeout in ms
	 * @return this config instance
	 */
	public BeerConfig setJettyIdleTimeout(Integer jettyIdleTimeout) {
		this.jettyIdleTimeout = jettyIdleTimeout;
		return this;
	}

	/**
	 * Gets the path to the SSL keystore file.
	 * 
	 * @return the keystore path
	 */
	public String getKeystorePath() {
		return keystorePath;
	}

	/**
	 * Sets the path to the SSL keystore file (enables HTTPS if set).
	 * 
	 * @param keystorePath the keystore path
	 * @return this config instance
	 */
	public BeerConfig setKeystorePath(String keystorePath) {
		this.keystorePath = keystorePath;
		return this;
	}

	/**
	 * Gets the password for the SSL keystore.
	 * 
	 * @return the keystore password
	 */
	public String getKeystorePass() {
		return keystorePass;
	}

	/**
	 * Sets the password for the SSL keystore.
	 * 
	 * @param keystorePass the keystore password
	 * @return this config instance
	 */
	public BeerConfig setKeystorePass(String keystorePass) {
		this.keystorePass = keystorePass;
		return this;
	}


	/**
	 * Gets the name of the logger to use.
	 * <p>
	 * SLF4J is used for logging.
	 *
	 * @return the logger name
	 */
	public String getLoggerName() {
		return loggerName;
	}

	/**
	 * Sets the name of the logger to use.
	 * <p>
	 * SLF4J is used for logging.
	 *
	 * @param loggerName the logger name
	 * @return this config instance
	 */
	public BeerConfig setLoggerName(String loggerName) {
		this.loggerName = loggerName;
		return this;
	}

}
