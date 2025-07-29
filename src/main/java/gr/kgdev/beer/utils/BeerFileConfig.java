package gr.kgdev.beer.utils;

public class BeerFileConfig {

	private Integer maxFileSize;
	private Integer maxReqSize;
	private Integer fileSizeThreshold;

	public Integer getMaxFileSize() {
		return maxFileSize;
	}

	public BeerFileConfig setMaxFileSize(Integer maxFileSize) {
		this.maxFileSize = maxFileSize;
		return this;
	}

	public Integer getMaxReqSize() {
		return maxReqSize;
	}

	public BeerFileConfig setMaxReqSize(Integer maxReqSize) {
		this.maxReqSize = maxReqSize;
		return this;
	}

	public Integer getFileSizeThreshold() {
		return fileSizeThreshold;
	}

	public BeerFileConfig setFileSizeThreshold(Integer fileSizeThreshold) {
		this.fileSizeThreshold = fileSizeThreshold;
		return this;
	}

}
