package com.st.maven.debian;

import java.util.Comparator;

class MappingPathComparator implements Comparator<Fileset> {
	
	static final MappingPathComparator INSTANCE = new MappingPathComparator();

	@Override
	public int compare(Fileset o1, Fileset o2) {
		return o1.getTarget().compareTo(o2.getTarget());
	}
	
}
