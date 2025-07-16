package gr.kgdev.beer.core;

public class BeerConfig {

	private String ip;
	private Integer port;
	private Integer jettyMinThreads;
	private Integer jettyMaxThreads;
	private Integer jettyIdleTimeout;
	private String keystorePath;
	private String keystorePass;
	private String loggerName;

	public String getIp() {
		return ip;
	}

	public BeerConfig setIp(String ip) {
		this.ip = ip;
		return this;
	}

	public Integer getPort() {
		return port;
	}

	public BeerConfig setPort(Integer port) {
		this.port = port;
		return this;
	}

	public Integer getJettyMinThreads() {
		return jettyMinThreads;
	}

	public BeerConfig setJettyMinThreads(Integer jettyMinThreads) {
		this.jettyMinThreads = jettyMinThreads;
		return this;
	}

	public Integer getJettyMaxThreads() {
		return jettyMaxThreads;
	}

	public BeerConfig setJettyMaxThreads(Integer jettyMaxThreads) {
		this.jettyMaxThreads = jettyMaxThreads;
		return this;
	}

	public Integer getJettyIdleTimeout() {
		return jettyIdleTimeout;
	}

	public BeerConfig setJettyIdleTimeout(Integer jettyIdleTimeout) {
		this.jettyIdleTimeout = jettyIdleTimeout;
		return this;
	}

	public String getKeystorePath() {
		return keystorePath;
	}

	public BeerConfig setKeystorePath(String keystorePath) {
		this.keystorePath = keystorePath;
		return this;
	}

	public String getKeystorePass() {
		return keystorePass;
	}

	public BeerConfig setKeystorePass(String keystorePass) {
		this.keystorePass = keystorePass;
		return this;
	}

	public String getLoggerName() {
		return loggerName;
	}

	public BeerConfig setLoggerName(String loggerName) {
		this.loggerName = loggerName;
		return this;
	}

}
