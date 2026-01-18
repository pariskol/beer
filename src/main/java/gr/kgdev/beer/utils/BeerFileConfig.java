
package gr.kgdev.beer.utils;

/**
 * Configuration class for file upload settings. Allows setting limits for file
 * size, request size, and caching behavior.
 */
public class BeerFileConfig {

	/**
	 * Maximum allowed size for a single uploaded file, in bytes.
	 */
	private Integer maxFileSize;

	/**
	 * Maximum allowed size for the entire upload request, in bytes.
	 */
	private Integer maxReqSize;

	/**
	 * Files larger than this threshold (in bytes) are written to disk instead of
	 * memory.
	 */
	private Integer fileSizeThreshold;

	/**
	 * Indicates whether caching is enabled for file uploads.
	 */
	private Boolean cacheEnabled = true;

	/**
	 * Returns the maximum file size in bytes.
	 */
	public Integer getMaxFileSize() {
		return maxFileSize;
	}

	/**
	 * Sets the maximum file size in bytes.
	 * 
	 * @param maxFileSize file size in bytes
	 * @return this config instance
	 */
	public BeerFileConfig setMaxFileSize(Integer maxFileSize) {
		this.maxFileSize = maxFileSize;
		return this;
	}

	/**
	 * Sets the maximum file size in megabytes (converted to bytes).
	 * 
	 * @param mb file size in megabytes
	 * @return this config instance
	 */
	public BeerFileConfig setMaxFileSizeMb(Integer mb) {
		this.maxFileSize = mb != null ? mb * 1024 * 1024 : null;
		return this;
	}

	/**
	 * Returns the maximum request size in bytes.
	 */
	public Integer getMaxReqSize() {
		return maxReqSize;
	}

	/**
	 * Sets the maximum request size in bytes.
	 * 
	 * @param maxReqSize request size in bytes
	 * @return this config instance
	 */
	public BeerFileConfig setMaxReqSize(Integer maxReqSize) {
		this.maxReqSize = maxReqSize;
		return this;
	}

	/**
	 * Sets the maximum request size in megabytes (converted to bytes).
	 * 
	 * @param mb request size in megabytes
	 * @return this config instance
	 */
	public BeerFileConfig setMaxReqSizeMb(Integer mb) {
		this.maxReqSize = mb != null ? mb * 1024 * 1024 : null;
		return this;
	}

	/**
	 * Returns the file size threshold in bytes.
	 */
	public Integer getFileSizeThreshold() {
		return fileSizeThreshold;
	}

	/**
	 * Sets the file size threshold in bytes.
	 * 
	 * @param fileSizeThreshold threshold in bytes
	 * @return this config instance
	 */
	public BeerFileConfig setFileSizeThreshold(Integer fileSizeThreshold) {
		this.fileSizeThreshold = fileSizeThreshold;
		return this;
	}

	/**
	 * Sets the file size threshold in megabytes (converted to bytes).
	 * 
	 * @param mb threshold in megabytes
	 * @return this config instance
	 */
	public BeerFileConfig setFileSizeThresholdMb(Integer mb) {
		this.fileSizeThreshold = mb != null ? mb * 1024 * 1024 : null;
		return this;
	}

	/**
	 * Returns whether caching is enabled.
	 */
	public Boolean getCacheEnabled() {
		return cacheEnabled;
	}

	/**
	 * Enables or disables caching for file uploads.
	 * 
	 * @param cacheEnabled true to enable caching, false to disable
	 * @return this config instance
	 */
	public BeerFileConfig setCacheEnabled(Boolean cacheEnabled) {
		this.cacheEnabled = cacheEnabled;
		return this;
	}

}
