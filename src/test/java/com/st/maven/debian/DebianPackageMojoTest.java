package com.st.maven.debian;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.code.ar.ArEntry;
import com.google.code.ar.ArInputStream;

public class DebianPackageMojoTest {

	@Rule
	public MojoRule mrule = new MojoRule();

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void testSuccess() throws Exception {
		File basedir = new File("src/test/resources/success");
		MavenProject mavenProject = mrule.readMavenProject(basedir);
		mavenProject.getBuild().setDirectory(folder.getRoot().getAbsolutePath());
		Mojo mm = mrule.lookupConfiguredMojo(mavenProject, "package");
		mm.execute();
		assertEquals(1, mavenProject.getAttachedArtifacts().size());
		Artifact artifact = mavenProject.getAttachedArtifacts().get(0);
		assertDeb(new File("src/test/resources/expected/success"), artifact.getFile(), artifact.getClassifier());
	}

	private static void assertDeb(File expectedDirectory, File actualFile, String expectedVersion) throws Exception {
		File[] contents = expectedDirectory.listFiles();
		Map<String, File> expectedFilenames = new HashMap<>();
		for (File cur : contents) {
			expectedFilenames.put(cur.getName(), cur);
		}
		try (ArInputStream aris = new ArInputStream(new BufferedInputStream(new FileInputStream(actualFile)))) {
			ArEntry curEntry = null;
			while ((curEntry = aris.getNextEntry()) != null) {
				if (curEntry.getFilename().equals("data.tar.gz")) {
					// skip for now
					continue;
				}
				File f = expectedFilenames.remove(curEntry.getFilename());
				assertNotNull("unexpected: " + curEntry.getFilename(), f);
				if (f.isFile()) {
					try (InputStream is = new FileInputStream(f)) {
						byte[] expected = IOUtils.toByteArray(is);
						assertArrayEquals(expected, curEntry.getData());
					}
				} else if (f.isDirectory()) {
					try (TarArchiveInputStream tar = new TarArchiveInputStream(new GZIPInputStream(new ByteArrayInputStream(curEntry.getData())))) {
						assertTar(null, f, tar, expectedVersion);
					}
				}
				if (expectedFilenames.isEmpty()) {
					break;
				}
			}
		}
		assertTrue("missing: " + expectedFilenames.toString(), expectedFilenames.isEmpty());
	}

	private static void assertTar(String currentDirectory, File expectedDirecotry, TarArchiveInputStream tar, String expectedVersion) throws Exception {
		if (currentDirectory != null) {
			currentDirectory = currentDirectory + "/";
		} else {
			currentDirectory = "";
		}
		TarArchiveEntry current = null;
		File[] expected = expectedDirecotry.listFiles();
		Map<String, File> expectedFilenames = new HashMap<>();
		for (File cur : expected) {
			String filename = currentDirectory + cur.getName();
			if (cur.isDirectory()) {
				filename = filename + "/";
			}
			expectedFilenames.put(filename, cur);
		}
		while ((current = tar.getNextTarEntry()) != null) {
			File f = expectedFilenames.remove(current.getName());
			assertNotNull("unexpected: " + current.getName(), f);
			if (f.isDirectory()) {
				assertTar(currentDirectory + f.getName(), f, tar, expectedVersion);
			} else {
				byte[] expectedData;
				try (InputStream is = new FileInputStream(f)) {
					expectedData = IOUtils.toByteArray(is);
				}
				byte[] actualData = new byte[(int) current.getSize()];
				org.apache.commons.compress.utils.IOUtils.readFully(tar, actualData);
				if (current.getName().equals("control")) {
					assertControl(expectedData, actualData, expectedVersion);
				} else {
					assertArrayEquals(f.getAbsolutePath(), expectedData, actualData);
				}
			}
			if (expectedFilenames.isEmpty()) {
				break;
			}
		}
		assertTrue("missing: " + expectedFilenames.toString(), expectedFilenames.isEmpty());
	}

	private static void assertControl(byte[] expected, byte[] actual, String expectedVersion) throws Exception {
		try (BufferedReader expectedBuf = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(expected), StandardCharsets.ISO_8859_1)); BufferedReader actualBuf = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(actual), StandardCharsets.ISO_8859_1))) {
			String expectedLine = null;
			String actualLine = null;
			while ((expectedLine = expectedBuf.readLine()) != null) {
				actualLine = actualBuf.readLine();
				assertNotNull(actualLine);
				if (expectedLine.startsWith("Version:")) {
					assertEquals(actualLine, "Version: " + expectedVersion);
					continue;
				}
				assertEquals(expectedLine, actualLine);
			}
			// no more lines in the actual file
			assertNull(actualBuf.readLine());
		}
	}

}
