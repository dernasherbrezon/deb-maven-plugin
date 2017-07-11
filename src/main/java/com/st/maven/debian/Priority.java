package com.st.maven.debian;

import java.util.Locale;

public enum Priority {
	
	REQUIRED, IMPORTANT, STANDARD, EXTRA;

	public String getName() {
		return name().toLowerCase(Locale.UK);
	}
	
}
