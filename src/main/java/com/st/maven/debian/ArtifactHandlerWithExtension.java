package com.st.maven.debian;

import org.apache.maven.artifact.handler.ArtifactHandler;

final class ArtifactHandlerWithExtension implements ArtifactHandler {

	private final String extension;
	private final ArtifactHandler handler;
	
	ArtifactHandlerWithExtension(String extension, ArtifactHandler handler) {
		this.extension = extension;
		this.handler = handler;
	}
	
	@Override
	public String getExtension() {
		return extension;
	}
	
	@Override
	public String getDirectory() {
		return handler.getDirectory();
	}

	@Override
	public String getClassifier() {
		return handler.getClassifier();
	}

	@Override
	public String getPackaging() {
		return handler.getPackaging();
	}

	@Override
	public boolean isIncludesDependencies() {
		return handler.isIncludesDependencies();
	}

	@Override
	public String getLanguage() {
		return handler.getLanguage();
	}

	@Override
	public boolean isAddedToClasspath() {
		return handler.isAddedToClasspath();
	}
	
}
