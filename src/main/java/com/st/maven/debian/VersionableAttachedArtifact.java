package com.st.maven.debian;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.InvalidArtifactRTException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;

public class VersionableAttachedArtifact extends DefaultArtifact {

	private final Artifact parent;
	private final String type;

	public VersionableAttachedArtifact(Artifact parent, String type, String classifier) {
		super(parent.getGroupId(), parent.getArtifactId(), parent.getVersionRange(), parent.getScope(), type, classifier, new ArtifactHandlerWithExtension(type, parent.getArtifactHandler()), parent.isOptional());
		this.parent = parent;

		if (type == null || type.trim().length() < 1) {
			throw new InvalidArtifactRTException(getGroupId(), getArtifactId(), getVersion(), type, "Attached artifacts must specify a type.");
		}

		if (classifier == null || classifier.trim().length() < 1) {
			throw new InvalidArtifactRTException(getGroupId(), getArtifactId(), getVersion(), type, "Attached artifacts must specify a classifier.");
		}
		
		this.type = type;
	}

	@Override
	public String getArtifactId() {
		return parent.getArtifactId();
	}

	@Override
	public List<ArtifactVersion> getAvailableVersions() {
		return parent.getAvailableVersions();
	}

	@Override
	public String getBaseVersion() {
		return parent.getBaseVersion();
	}

	@Override
	public ArtifactFilter getDependencyFilter() {
		return parent.getDependencyFilter();
	}

	@Override
	public List<String> getDependencyTrail() {
		return parent.getDependencyTrail();
	}

	@Override
	public String getDownloadUrl() {
		return parent.getDownloadUrl();
	}

	@Override
	public String getGroupId() {
		return parent.getGroupId();
	}

	@Override
	public ArtifactRepository getRepository() {
		return parent.getRepository();
	}

	@Override
	public String getScope() {
		return parent.getScope();
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public String getVersion() {
		return parent.getVersion();
	}

	@Override
	public VersionRange getVersionRange() {
		return parent.getVersionRange();
	}

	@Override
	public boolean isOptional() {
		return parent.isOptional();
	}

	@Override
	public boolean isRelease() {
		return parent.isRelease();
	}

	@Override
	public boolean isSnapshot() {
		return parent.isSnapshot();
	}
	
	@Override
	public boolean equals(Object o) {
		return super.equals(o);
	}
	
	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
