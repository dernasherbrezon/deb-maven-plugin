package com.st.maven.debian;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.maven.model.Developer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import com.google.code.ar.ArEntry;
import com.google.code.ar.ArFileOutputStream;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

/**
 * @goal package
 * @phase install
 */
public class DebianPackageMojo extends AbstractMojo {

	/**
	 * The maven project.
	 * 
	 * @parameter property="project"
	 * @readonly
	 */
	private MavenProject project;

	/**
	 * @parameter
	 */
	private Map<Object, Object> osDependencies;

	/**
	 * @parameter
	 */
	private List<Fileset> fileSets;

	/**
	 * @parameter
	 */
	private String section;

	/**
	 * @parameter
	 */
	private String arch;

	/**
	 * @parameter
	 */
	private String priority;

	/**
	 * @parameter
	 * @required
	 */
	private String unixUserId;

	/**
	 * @parameter
	 * @required
	 */
	private String unixGroupId;

	/**
	 * @parameter default-value=true;
	 */
	private Boolean javaServiceWrapper;

	/**
	 * @parameter default-value=true;
	 */
	private Boolean attachArtifact;

	private final static String BASE_DIR = "./src/main/deb";
	private final static Pattern email = Pattern.compile("^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");
	private Configuration freemarkerConfig = new Configuration();
	private Set<String> dirsAdded = new HashSet<String>();
	private Set<String> ignore = new HashSet<String>();

	@Override
	public void execute() throws MojoExecutionException {

		validate();
		fillDefaults();

		freemarkerConfig.setDefaultEncoding("UTF-8");
		freemarkerConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		freemarkerConfig.setClassForTemplateLoading(DebianPackageMojo.class, "/");
		freemarkerConfig.setTimeZone(TimeZone.getTimeZone("GMT"));

		Config config = new Config();
		config.setArtifactId(project.getArtifactId());
		config.setDescription(project.getDescription());
		config.setGroup(unixGroupId);
		config.setUser(unixUserId);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		config.setVersion(sdf.format(new Date()));
		Developer dev = project.getDevelopers().get(0);
		String maintainer = dev.getName() + " <" + dev.getEmail() + ">";
		config.setMaintainer(maintainer);
		config.setName(project.getName());
		config.setDescription(project.getDescription());
		config.setDepends(formatDependencies(osDependencies));
		config.setInstallDir("/home/" + unixUserId + "/" + project.getArtifactId());
		if (section == null || section.trim().length() == 0) {
			config.setSection("java");
		} else {
			config.setSection(section);
		}
		if (arch == null || arch.trim().length() == 0) {
			config.setArch("all");
		} else {
			config.setArch(arch);
		}
		if (priority == null || priority.trim().length() == 0) {
			config.setPriority(Priority.STANDARD);
		} else {
			config.setPriority(Priority.valueOf(priority.toUpperCase(Locale.UK)));
		}

		ArFileOutputStream aros = null;
		try {
			File debFile = new File(project.getBuild().getDirectory() + File.separator + project.getArtifactId() + "-" + config.getVersion() + ".deb");
			getLog().info("Building deb: " + debFile.getAbsolutePath());
			aros = new ArFileOutputStream(debFile.getAbsolutePath());
			aros.putNextEntry(createEntry("debian-binary"));
			aros.write("2.0\n".getBytes(StandardCharsets.US_ASCII));
			aros.closeEntry();
			aros.putNextEntry(createEntry("control.tar.gz"));
			fillControlTar(config, aros);
			aros.closeEntry();
			aros.putNextEntry(createEntry("data.tar.gz"));
			fillDataTar(config, aros);
			aros.closeEntry();
			if (attachArtifact) {
				VersionableAttachedArtifact artifact = new VersionableAttachedArtifact(project.getArtifact(), "deb", config.getVersion());
				artifact.setFile(debFile);
				artifact.setResolved(true);
				project.addAttachedArtifact(artifact);
			}
		} catch (Exception e) {
			throw new MojoExecutionException("unable to create .deb file", e);
		} finally {
			if (aros != null) {
				try {
					aros.close();
				} catch (IOException e) {
					throw new MojoExecutionException("unable to close .deb file", e);
				}
			}
		}
	}

	private void fillDefaults() {
		unixUserId = unixUserId.trim();
		if (osDependencies == null) {
			osDependencies = new HashMap<>();
		}
		if (Boolean.TRUE.equals(javaServiceWrapper) && !osDependencies.containsKey("service-wrapper")) {
			osDependencies.put("service-wrapper", null);
		}
		ignore.add(".svn");
		ignore.add("preinst");
		ignore.add("postinst");
		ignore.add("prerm");
		ignore.add("postrm");
	}

	private void validate() throws MojoExecutionException {
		if (unixUserId == null || unixUserId.trim().length() == 0) {
			throw new MojoExecutionException("unixUserId should be specified");
		}
		if (unixUserId.trim().length() > 8) {
			throw new MojoExecutionException("unixUserId lenght should be less than 8");
		}
		File debDir = new File(BASE_DIR);
		if (!debDir.exists()) {
			throw new MojoExecutionException(".deb base directory doesnt exist: " + BASE_DIR);
		}
		if (project.getDescription() == null || project.getDescription().trim().length() == 0) {
			throw new MojoExecutionException("project description is mandatory");
		}
		if (project.getDevelopers() == null || project.getDevelopers().isEmpty()) {
			throw new MojoExecutionException("project maintainer is mandatory. Please specify valid \"developers\" entry");
		}
		Developer dev = project.getDevelopers().get(0);
		if (dev == null) {
			throw new MojoExecutionException("project maintainer is mandatory. Please specify valid \"developers\" entry");
		}
		if (dev.getName() == null || dev.getName().trim().length() == 0) {
			throw new MojoExecutionException("project maintainer name is mandatory. Please fill valid developer name");
		}
		if (dev.getEmail() == null || dev.getEmail().trim().length() == 0) {
			throw new MojoExecutionException("project maintainer email is mandatory. Please specify valid developer email");
		}
		Matcher m = email.matcher(dev.getEmail());
		if (!m.matches()) {
			throw new MojoExecutionException("invalid project maintainer email: " + dev.getEmail());
		}
	}

	private void fillDataTar(Config config, ArFileOutputStream output) throws MojoExecutionException {
		TarArchiveOutputStream tar = null;
		try {
			tar = new TarArchiveOutputStream(new GZIPOutputStream(new ArWrapper(output)));
			tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
			if (Boolean.TRUE.equals(javaServiceWrapper)) {
				byte[] daemonData = processTemplate(freemarkerConfig, config, "daemon.ftl");
				TarArchiveEntry initScript = new TarArchiveEntry("etc/init.d/" + project.getArtifactId());
				initScript.setSize(daemonData.length);
				initScript.setMode(040755);
				tar.putArchiveEntry(initScript);
				tar.write(daemonData);
				tar.closeArchiveEntry();
			}
			String packageBaseDir = "home/" + unixUserId + "/" + project.getArtifactId() + "/";
			if (fileSets != null && !fileSets.isEmpty()) {
				writeDirectory(tar, packageBaseDir);

				Collections.sort(fileSets, MappingPathComparator.INSTANCE);
				for (Fileset curPath : fileSets) {
					curPath.setTarget(packageBaseDir + curPath.getTarget());
					addRecursively(config, tar, curPath);
				}
			}

		} catch (Exception e) {
			throw new MojoExecutionException("unable to create data tar", e);
		} finally {
			IOUtils.closeQuietly(tar);
		}
	}

	private void addRecursively(Config config, TarArchiveOutputStream tar, Fileset fileset) throws MojoExecutionException {
		File sourceFile = new File(fileset.getSource());
		String targetFilename = fileset.getTarget();
		// skip well-known ignore directories
		if (ignore.contains(sourceFile.getName()) || sourceFile.getName().endsWith(".rrd") || sourceFile.getName().endsWith(".log")) {
			return;
		}
		FileInputStream fis = null;
		try {
			if (!sourceFile.isDirectory()) {
				TarArchiveEntry curEntry = new TarArchiveEntry(targetFilename);
				if (fileset.isFilter()) {
					byte[] bytes = processTemplate(freemarkerConfig, config, fileset.getSource());
					curEntry.setSize(bytes.length);
					tar.putArchiveEntry(curEntry);
					tar.write(bytes);
				} else {
					curEntry.setSize(sourceFile.length());
					tar.putArchiveEntry(curEntry);
					fis = new FileInputStream(sourceFile);
					IOUtils.copy(fis, tar);
				}
				tar.closeArchiveEntry();
			} else if (sourceFile.isDirectory()) {
				targetFilename += "/";
				if (!dirsAdded.contains(targetFilename)) {
					dirsAdded.add(targetFilename);
					writeDirectory(tar, targetFilename);
				}
			}
		} catch (Exception e) {
			throw new MojoExecutionException("unable to write", e);
		} finally {
			IOUtils.closeQuietly(fis);
		}

		if (sourceFile.isDirectory()) {
			File[] subFiles = sourceFile.listFiles();
			for (File curSubFile : subFiles) {
				Fileset curSubFileset = new Fileset(fileset.getSource() + "/" + curSubFile.getName(), fileset.getTarget() + "/" + curSubFile.getName(), fileset.isFilter());
				addRecursively(config, tar, curSubFileset);
			}
		}
	}

	private static void writeDirectory(TarArchiveOutputStream tar, String dirName) throws MojoExecutionException {
		try {
			if (!dirName.endsWith("/")) {
				dirName = dirName + "/";
			}
			TarArchiveEntry curEntry = new TarArchiveEntry(dirName);
			tar.putArchiveEntry(curEntry);
			tar.closeArchiveEntry();
		} catch (Exception e) {
			throw new MojoExecutionException("unable to add directory: " + dirName, e);
		}
	}

	private void fillControlTar(Config config, ArFileOutputStream output) throws MojoExecutionException {
		TarArchiveOutputStream tar = null;
		try {
			tar = new TarArchiveOutputStream(new GZIPOutputStream(new ArWrapper(output)));
			tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
			TarArchiveEntry rootDir = new TarArchiveEntry("./");
			tar.putArchiveEntry(rootDir);
			tar.closeArchiveEntry();

			byte[] controlData = processTemplate(freemarkerConfig, config, "control.ftl");
			TarArchiveEntry controlEntry = new TarArchiveEntry("./control");
			controlEntry.setSize(controlData.length);
			tar.putArchiveEntry(controlEntry);
			tar.write(controlData);
			tar.closeArchiveEntry();

			byte[] preinstBaseData = processTemplate("preinst", freemarkerConfig, config, combine("preinst.ftl", BASE_DIR + File.separator + "preinst", false));
			long size = preinstBaseData.length;
			TarArchiveEntry preinstEntry = new TarArchiveEntry("./preinst");
			preinstEntry.setSize(size);
			preinstEntry.setMode(0755);
			tar.putArchiveEntry(preinstEntry);
			tar.write(preinstBaseData);
			tar.closeArchiveEntry();

			byte[] postinstBaseData = processTemplate("postinst", freemarkerConfig, config, combine("postinst.ftl", BASE_DIR + File.separator + "postinst", true));
			size = postinstBaseData.length;
			TarArchiveEntry postinstEntry = new TarArchiveEntry("./postinst");
			postinstEntry.setSize(size);
			postinstEntry.setMode(0755);
			tar.putArchiveEntry(postinstEntry);
			tar.write(postinstBaseData);
			tar.closeArchiveEntry();

			byte[] prermBaseData = processTemplate("prerm", freemarkerConfig, config, combine("prerm.ftl", BASE_DIR + File.separator + "prerm", false));
			size = prermBaseData.length;
			TarArchiveEntry prermEntry = new TarArchiveEntry("./prerm");
			prermEntry.setSize(size);
			prermEntry.setMode(0755);
			tar.putArchiveEntry(prermEntry);
			tar.write(prermBaseData);
			tar.closeArchiveEntry();

			byte[] postrmBaseData = processTemplate("postrm", freemarkerConfig, config, combine("postrm.ftl", BASE_DIR + File.separator + "postrm", false));
			size = postrmBaseData.length;
			TarArchiveEntry postrmEntry = new TarArchiveEntry("./postrm");
			postrmEntry.setSize(size);
			postrmEntry.setMode(0755);
			tar.putArchiveEntry(postrmEntry);
			tar.write(postrmBaseData);
			tar.closeArchiveEntry();

		} catch (Exception e) {
			throw new MojoExecutionException("unable to create control tar", e);
		} finally {
			if (tar != null) {
				try {
					tar.close();
				} catch (IOException e) {
					getLog().error("unable to finish tar", e);
				}
			}
		}
	}

	private static String combine(String classpathResource, String file, boolean inverse) throws MojoExecutionException {
		StringBuilder builder = new StringBuilder();
		builder.append("#!/bin/bash -e\n");
		if (inverse) {
			appendUserScript(file, builder);
			appendSystemScript(classpathResource, builder);
		} else {
			appendSystemScript(classpathResource, builder);
			appendUserScript(file, builder);
		}
		return builder.toString();
	}

	private static void appendUserScript(String file, StringBuilder builder) throws MojoExecutionException {
		BufferedReader r = null;
		try {
			File f = new File(file);
			String curLine = null;
			if (f.exists()) {
				r = new BufferedReader(new FileReader(file));
				while ((curLine = r.readLine()) != null) {
					builder.append(curLine).append('\n');
				}
			}
		} catch (Exception e) {
			throw new MojoExecutionException("unable to combine data", e);
		} finally {
			IOUtils.closeQuietly(r);
		}
	}

	private static void appendSystemScript(String classpathResource, StringBuilder builder) throws MojoExecutionException {
		BufferedReader isr = null;
		try {
			isr = new BufferedReader(new InputStreamReader(DebianPackageMojo.class.getClassLoader().getResourceAsStream(classpathResource), "UTF-8"));
			String curLine = null;
			while ((curLine = isr.readLine()) != null) {
				builder.append(curLine).append('\n');
			}
		} catch (Exception e) {
			throw new MojoExecutionException("unable to combine data", e);
		} finally {
			IOUtils.closeQuietly(isr);
		}
	}

	private static String formatDependencies(Map<Object, Object> osDependencies) {
		StringBuilder result = new StringBuilder();
		if (osDependencies != null && !osDependencies.isEmpty()) {
			Iterator<Object> keys = osDependencies.keySet().iterator();
			while (keys.hasNext()) {
				Object key = keys.next();
				if (key == null) {
					continue;
				}
				Object value = osDependencies.get(key);
				if (result.length() != 0) {
					result.append(", ");
				}
				result.append(key.toString());
				if (value != null) {
					result.append(" (").append(value.toString()).append(")");
				}
			}
		}
		return result.toString();
	}

	private static byte[] processTemplate(Configuration freemarkerConfig, Config config, String name) throws IOException, TemplateException {
		Template fTemplate = freemarkerConfig.getTemplate(name);
		return processTemplate(config, fTemplate);
	}

	private static byte[] processTemplate(String name, Configuration freemarkerConfig, Config config, String filedata) throws TemplateException, IOException {
		Template fTemplate = new Template(name, new StringReader(filedata), freemarkerConfig);
		return processTemplate(config, fTemplate);
	}

	private static byte[] processTemplate(Config config, Template fTemplate) throws TemplateException, IOException {
		StringWriter w = new StringWriter();
		Map<String, Object> data = new HashMap<>();
		data.put("config", config);
		fTemplate.process(data, w);
		return w.toString().getBytes(StandardCharsets.UTF_8);
	}

	private static ArEntry createEntry(String name) {
		ArEntry result = new ArEntry();
		result.setOwnerId(0);
		result.setGroupId(0);
		result.setFilename(name);
		result.setFileMode(100644);
		return result;
	}
}
