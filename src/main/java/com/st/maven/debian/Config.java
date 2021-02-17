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
	private String installDir;
	private String section;
	private String arch;
	private Priority priority;
	private String sourceUrl;
	private String copyright;
	private LicenseName licenseName;
	private String customCopyRightFile;
	private Boolean javaServiceWrapper;

	public String getCustomCopyRightFile() {
		return customCopyRightFile;
	}

	public void setCustomCopyRightFile(String customCopyRightFile) {
		this.customCopyRightFile = customCopyRightFile;
	}

	public Boolean getJavaServiceWrapper() {
		return javaServiceWrapper;
	}
	
	public void setJavaServiceWrapper(Boolean javaServiceWrapper) {
		this.javaServiceWrapper = javaServiceWrapper;
	}
	
	public LicenseName getLicenseName() {
		return licenseName;
	}
	
	public void setLicenseName(LicenseName licenseName) {
		this.licenseName = licenseName;
	}

	public String getCopyright() {
		return copyright;
	}

	public void setCopyright(String copyright) {
		this.copyright = copyright;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	public void setSourceUrl(String sourceUrl) {
		this.sourceUrl = sourceUrl;
	}

	public Priority getPriority() {
		return priority;
	}

	public void setPriority(Priority priority) {
		this.priority = priority;
	}

	public String getArch() {
		return arch;
	}

	public void setArch(String arch) {
		this.arch = arch;
	}

	public String getSection() {
		return section;
	}

	public void setSection(String section) {
		this.section = section;
	}

	public String getInstallDir() {
		return installDir;
	}

	public void setInstallDir(String installDir) {
		this.installDir = installDir;
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
