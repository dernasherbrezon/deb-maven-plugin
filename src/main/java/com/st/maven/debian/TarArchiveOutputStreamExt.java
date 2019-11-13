package com.st.maven.debian;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

public class TarArchiveOutputStreamExt extends TarArchiveOutputStream {

	private final Set<String> addedPaths = new HashSet<>();

	public TarArchiveOutputStreamExt(OutputStream os) {
		super(os);
	}

	public void writeEntry(TarArchiveEntry entry, InputStream data) throws IOException {
		ensureDirectoriesAdded(entry.getName());
		putArchiveEntry(entry);
		IOUtils.copy(data, this);
		closeArchiveEntry();
	}

	public void writeEntry(TarArchiveEntry entry, byte[] data) throws IOException {
		ensureDirectoriesAdded(entry.getName());
		if (entry.getName().charAt(entry.getName().length() - 1) == '/') {
			return;
		}
		putArchiveEntry(entry);
		if (data != null) {
			write(data);
		}
		closeArchiveEntry();
	}

	private void ensureDirectoriesAdded(String fullPath) throws IOException {
		int index = -1;
		while ((index = fullPath.indexOf('/', index + 1)) != -1) {
			String curDirectory = fullPath.substring(0, index);
			if (addedPaths.contains(curDirectory)) {
				continue;
			}
			TarArchiveEntry curEntry = new TarArchiveEntry(curDirectory + "/");
			putArchiveEntry(curEntry);
			closeArchiveEntry();
			addedPaths.add(curDirectory);
		}
	}
}
