package com.st.maven.debian;

public class Fileset {

	public String source;
	public String target;
	public boolean filter;
	
	public Fileset() {
		//do nothing
	}

	public Fileset(String source, String target, boolean filter) {
		super();
		this.source = source;
		this.target = target;
		this.filter = filter;
	}

	public boolean isFilter() {
		return filter;
	}

	public void setFilter(boolean filter) {
		this.filter = filter;
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
