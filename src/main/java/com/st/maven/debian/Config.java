package com.st.maven.debian;

public class Config {

	private String artifactId;
	private String user;
	private String group;
	private String name;
	private String description;
	private String version;
	private String maintainer;
	private String depends;
	private String wrapperConfig;
	
	public String getWrapperConfig() {
		return wrapperConfig;
	}
	
	public void setWrapperConfig(String wrapperConfig) {
		this.wrapperConfig = wrapperConfig;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getMaintainer() {
		return maintainer;
	}

	public void setMaintainer(String maintainer) {
		this.maintainer = maintainer;
	}

	public String getDepends() {
		return depends;
	}

	public void setDepends(String depends) {
		this.depends = depends;
	}

	public void setVersion(String version) {
		this.version = version;
	}
	
	public String getVersion() {
		return version;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
