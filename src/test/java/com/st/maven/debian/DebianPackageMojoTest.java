package com.st.maven.debian;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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
	}

}
