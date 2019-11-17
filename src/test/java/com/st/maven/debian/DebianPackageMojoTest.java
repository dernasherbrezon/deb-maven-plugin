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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
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

	@Test(expected = MojoExecutionException.class)
	public void testInvalidPackageName() throws Exception {
		MavenProject mavenProject = loadSuccessProject();
		mavenProject.setArtifactId("sample_project");
		Mojo mm = mrule.lookupConfiguredMojo(mavenProject, "package");
		mm.execute();
	}

	@Test(expected = MojoExecutionException.class)
	public void testInvalidUser() throws Exception {
		MavenProject mavenProject = loadSuccessProject();
		Mojo mm = mrule.lookupConfiguredMojo(mavenProject, "package");
		mrule.setVariableValueToObject(mm, "unixUserId", " ");
		mm.execute();
	}

	@Test(expected = MojoExecutionException.class)
	public void testInvalidInstallDir() throws Exception {
		MavenProject mavenProject = loadSuccessProject();
		Mojo mm = mrule.lookupConfiguredMojo(mavenProject, "package");
		mrule.setVariableValueToObject(mm, "installDir", "opt/local/");
		mm.execute();
	}

	@Test(expected = MojoExecutionException.class)
	public void testInvalidYear() throws Exception {
		MavenProject mavenProject = loadSuccessProject();
		mavenProject.setInceptionYear(null);
		Mojo mm = mrule.lookupConfiguredMojo(mavenProject, "package");
		mm.execute();
	}

	@Test(expected = MojoExecutionException.class)
	public void testInvalidDescription() throws Exception {
		MavenProject mavenProject = loadSuccessProject();
		mavenProject.setDescription(" ");
		Mojo mm = mrule.lookupConfiguredMojo(mavenProject, "package");
		mm.execute();
	}

	@Test(expected = MojoExecutionException.class)
	public void testEmptyLicense() throws Exception {
		MavenProject mavenProject = loadSuccessProject();
		mavenProject.setLicenses(Collections.emptyList());
		Mojo mm = mrule.lookupConfiguredMojo(mavenProject, "package");
		mm.execute();
	}

	@Test(expected = MojoExecutionException.class)
	public void testUnknownLicense() throws Exception {
		License license = new License();
		license.setName(UUID.randomUUID().toString());
		MavenProject mavenProject = loadSuccessProject();
		mavenProject.setLicenses(Collections.singletonList(license));
		Mojo mm = mrule.lookupConfiguredMojo(mavenProject, "package");
		mm.execute();
	}

	@Test(expected = MojoExecutionException.class)
	public void testMissingDeveloper() throws Exception {
		MavenProject mavenProject = loadSuccessProject();
		mavenProject.setDevelopers(Collections.emptyList());
		Mojo mm = mrule.lookupConfiguredMojo(mavenProject, "package");
		mm.execute();
	}

	@Test(expected = MojoExecutionException.class)
	public void testInvalidDeveloper() throws Exception {
		Developer dev = createDeveloper();
		dev.setName(" ");
		MavenProject mavenProject = loadSuccessProject();
		mavenProject.setDevelopers(Collections.singletonList(dev));
		Mojo mm = mrule.lookupConfiguredMojo(mavenProject, "package");
		mm.execute();
	}
	
	@Test(expected = MojoExecutionException.class)
	public void testInvalidDeveloper2() throws Exception {
		Developer dev = createDeveloper();
		dev.setEmail(" ");
		MavenProject mavenProject = loadSuccessProject();
		mavenProject.setDevelopers(Collections.singletonList(dev));
		Mojo mm = mrule.lookupConfiguredMojo(mavenProject, "package");
		mm.execute();
	}
	
	@Test(expected = MojoExecutionException.class)
	public void testInvalidDeveloper3() throws Exception {
		Developer dev = createDeveloper();
		dev.setEmail(UUID.randomUUID().toString());
		MavenProject mavenProject = loadSuccessProject();
		mavenProject.setDevelopers(Collections.singletonList(dev));
		Mojo mm = mrule.lookupConfiguredMojo(mavenProject, "package");
		mm.execute();
	}

	@Test
	public void testVersionFromPom() throws Exception {
		MavenProject mavenProject = loadSuccessProject();
		String version = UUID.randomUUID().toString();
		mavenProject.setVersion(version);
		Mojo mm = mrule.lookupConfiguredMojo(mavenProject, "package");
		mrule.setVariableValueToObject(mm, "generateVersion", false);
		mm.execute();
		assertEquals(1, mavenProject.getAttachedArtifacts().size());
		Artifact artifact = mavenProject.getAttachedArtifacts().get(0);
		assertDeb(new File("src/test/resources/expected/success"), artifact.getFile(), version);
	}

	@Test
	public void testSuccess() throws Exception {
		MavenProject mavenProject = loadSuccessProject();
		Mojo mm = mrule.lookupConfiguredMojo(mavenProject, "package");
		mm.execute();
		assertEquals(1, mavenProject.getAttachedArtifacts().size());
		Artifact artifact = mavenProject.getAttachedArtifacts().get(0);
		assertDeb(new File("src/test/resources/expected/success"), artifact.getFile(), artifact.getClassifier());
	}

	private static Developer createDeveloper() {
		Developer dev = new Developer();
		dev.setName(UUID.randomUUID().toString());
		dev.setEmail("test@example.com");
		return dev;
	}
	
	private MavenProject loadSuccessProject() throws Exception {
		File basedir = new File("src/test/resources/success");
		MavenProject mavenProject = mrule.readMavenProject(basedir);
		mavenProject.getBuild().setDirectory(folder.getRoot().getAbsolutePath());
		return mavenProject;
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
				File f = expectedFilenames.remove(curEntry.getFilename());
				assertNotNull("unexpected: " + curEntry.getFilename(), f);
				if (f.isFile()) {
					try (InputStream is = new FileInputStream(f)) {
						byte[] expected = IOUtils.toByteArray(is);
						assertArrayEquals(expected, curEntry.getData());
					}
				} else if (f.isDirectory()) {
					Map<String, File> allExpected = findExpected(null, f);
					try (TarArchiveInputStream tar = new TarArchiveInputStream(new GZIPInputStream(new ByteArrayInputStream(curEntry.getData())))) {
						assertTar(allExpected, tar, expectedVersion);
					}
				}
				if (expectedFilenames.isEmpty()) {
					break;
				}
			}
		}
		assertTrue("missing: " + expectedFilenames.toString(), expectedFilenames.isEmpty());
	}

	private static Map<String, File> findExpected(String currentDirectory, File expectedDirecotry) {
		if (currentDirectory != null) {
			currentDirectory = currentDirectory + "/";
		} else {
			currentDirectory = "";
		}
		File[] expected = expectedDirecotry.listFiles();
		Map<String, File> expectedFilenames = new HashMap<>();
		for (File cur : expected) {
			String filename = currentDirectory + cur.getName();
			if (cur.isDirectory()) {
				filename = filename + "/";
			}
			expectedFilenames.put(filename, cur);
			if (cur.isDirectory()) {
				expectedFilenames.putAll(findExpected(currentDirectory + cur.getName(), cur));
			}
		}
		return expectedFilenames;
	}

	private static void assertTar(Map<String, File> allExpected, TarArchiveInputStream tar, String expectedVersion) throws Exception {
		TarArchiveEntry current = null;
		while ((current = tar.getNextTarEntry()) != null) {
			File f = allExpected.remove(current.getName());
			assertNotNull("unexpected: " + current.getName(), f);
			if (!f.isDirectory()) {
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
			if (allExpected.isEmpty()) {
				break;
			}
		}
		assertTrue("missing: " + allExpected.toString(), allExpected.isEmpty());
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
