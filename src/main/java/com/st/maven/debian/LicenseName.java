package com.st.maven.debian;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public enum LicenseName {

	// taken from /usr/share/common-licenses
	APACHE20("Apache-2.0"), ARTISTIC("Artistic"), BSD("BSD"), GFDL("GFDL"), GFDL12("GFDL-1.2"), GFDL13("GFDL-1.3"), GPL("GPL"), GPL1("GPL-1"), GPL2("GPL-2"), GPL3("GPL-3"), LGPL("LGPL"), LGPL2("LGPL-2"), LGPL21("LGPL-2.1"), LGPL3("LGPL-3");

	private final String shortName;
	private static final Map<String, LicenseName> ALL = new HashMap<>();

	static {
		for (LicenseName cur : LicenseName.values()) {
			ALL.put(cur.getShortName(), cur);
		}
	}

	private LicenseName(String shortName) {
		this.shortName = shortName;
	}

	public String getShortName() {
		return shortName;
	}

	public static LicenseName valueOfShortName(String shortName) {
		return ALL.get(shortName);
	}

	public static Set<String> getAllShortNames() {
		return ALL.keySet();
	}
}
