package com.st.maven.debian;

public class Fileset {

	private String source;
	private String target;
	
	public Fileset() {
		//do nothing
	}

	public Fileset(String source, String target) {
		super();
		this.source = source;
		this.target = target;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

}
