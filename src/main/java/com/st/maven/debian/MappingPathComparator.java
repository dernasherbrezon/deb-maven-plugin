package com.st.maven.debian;

import java.util.Comparator;

class MappingPathComparator implements Comparator<MappingPath> {

	@Override
	public int compare(MappingPath o1, MappingPath o2) {
		return o1.getTargetPath().compareTo(o2.getTargetPath());
	}
	
}
