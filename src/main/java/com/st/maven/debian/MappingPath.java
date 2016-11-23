package com.st.maven.debian;

class MappingPath {

	private String sourcePath;
	private String targetPath;

	public MappingPath(String sourcePath, String targetPath) {
		this.sourcePath = sourcePath;
		this.targetPath = targetPath;
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	public String getTargetPath() {
		return targetPath;
	}

	public void setTargetPath(String targetPath) {
		this.targetPath = targetPath;
	}

}
