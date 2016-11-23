package com.st.maven.debian;

import java.io.IOException;
import java.io.OutputStream;

import com.google.code.ar.ArFileOutputStream;

class ArWrapper extends OutputStream {

	private final ArFileOutputStream fos;
	
	ArWrapper(ArFileOutputStream fos) {
		this.fos = fos;
	}
	
	@Override
	public void write(int b) throws IOException {
		fos.write(b);
	}
	
	
	@Override
	public void write(byte[] b) throws IOException {
		fos.write(b);
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		fos.write(b, off, len);
	}
	
	
}
