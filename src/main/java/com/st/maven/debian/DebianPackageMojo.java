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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
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
import org.apache.maven.model.Developer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.google.code.ar.ArEntry;
import com.google.code.ar.ArFileOutputStream;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

@Mojo(name = "package", defaultPhase = LifecyclePhase.INSTALL)
public class DebianPackageMojo extends AbstractMojo {

	private static final String POSTRM = "postrm";
	private static final String PRERM = "prerm";
	private static final String POSTINST = "postinst";
	private static final String PREINST = "preinst";

	@Parameter(defaultValue = "${project}", readonly = true)
	private MavenProject project;

	@Parameter
	private Map<Object, Object> osDependencies;

	@Parameter
	private String installDir;

	@Parameter
	private List<Fileset> fileSets;

	@Parameter
	private String section;

	@Parameter
	private String arch;

	@Parameter
	private String priority;

	@Parameter(required = true)
	private String unixUserId;

	@Parameter(required = true)
	private String unixGroupId;

	@Parameter(defaultValue = "true")
	private Boolean javaServiceWrapper;

	@Parameter(defaultValue = "true")
	private boolean attachArtifact;

	@Parameter(defaultValue = "true")
	private Boolean generateVersion;

	@Parameter(defaultValue = "${project.basedir}/src/main/deb")
	private String debBaseDir;

	@Parameter
	private String customCopyRightFile;

	private static final Pattern EMAIL = Pattern
			.compile("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])");
	private static final Pattern PACKAGE_NAME = Pattern.compile("^[a-z0-9][a-z0-9\\.\\+\\-]+$");
	private final Configuration freemarkerConfig = new Configuration(Configuration.VERSION_2_3_0);
	private final Set<String> ignore = new HashSet<>();

	@Override
	public void execute() throws MojoExecutionException {

		validate();
		fillDefaults();

		freemarkerConfig.setDefaultEncoding("UTF-8");
		freemarkerConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		freemarkerConfig.setClassForTemplateLoading(DebianPackageMojo.class, "/");
		freemarkerConfig.setTimeZone(TimeZone.getTimeZone("GMT"));

		Config config = setupConfig();

		File debFile = new File(project.getBuild().getDirectory() + File.separator + project.getArtifactId() + "-" + config.getVersion() + ".deb");

		// sometimes ./target/ directory might not exist
		// for example with turned off jar/install/deploy plugins
		if (!debFile.getParentFile().exists() && !debFile.getParentFile().mkdirs()) {
			throw new MojoExecutionException("unable to create parent directory for the .deb at: " + debFile.getAbsolutePath());
		}
		getLog().info("Building deb: " + debFile.getAbsolutePath());
		try (ArFileOutputStream aros = new ArFileOutputStream(debFile.getAbsolutePath())) {
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
		}
	}

	private Config setupConfig() {
		Config config = new Config();
		config.setArtifactId(project.getArtifactId().trim());
		config.setDescription(project.getDescription().trim());
		config.setGroup(unixGroupId);
		config.setUser(unixUserId);
		config.setJavaServiceWrapper(javaServiceWrapper);
		config.setVersion(setupVersion());
		Developer dev = project.getDevelopers().get(0);
		String maintainer = dev.getName() + " <" + dev.getEmail() + ">";
		config.setMaintainer(maintainer);
		config.setName(project.getName());
		config.setDescription(project.getDescription());
		config.setDepends(formatDependencies(osDependencies));
		config.setInstallDir(composeInstallDir());
		if (isNullOrBlank(section)) {
			config.setSection("java");
		} else {
			config.setSection(section);
		}
		if (isNullOrBlank(arch)) {
			config.setArch("all");
		} else {
			config.setArch(arch.trim());
		}
		if (isNullOrBlank(priority)) {
			config.setPriority(Priority.STANDARD);
		} else {
			config.setPriority(Priority.valueOf(priority.toUpperCase(Locale.UK)));
		}
		if (project.getScm() != null && !isNullOrBlank(project.getScm().getUrl())) {
			config.setSourceUrl(project.getScm().getUrl().trim());
		}
		if (project.getOrganization() != null && !isNullOrBlank(project.getOrganization().getName())) {
			config.setCopyright(project.getInceptionYear().trim() + ", " + project.getOrganization().getName().trim());
		} else {
			config.setCopyright(project.getInceptionYear().trim() + ", " + dev.getName().trim());
		}
		config.setLicenseName(LicenseName.valueOfShortName(project.getLicenses().get(0).getName()));
		if (!isNullOrBlank(customCopyRightFile)) {
			config.setCustomCopyRightFile(customCopyRightFile.trim());
		}
		return config;
	}

	private boolean isNullOrBlank(String string) {
		return string == null || string.trim().isEmpty();
	}

	private String setupVersion() {
		String version;
		if (generateVersion != null && generateVersion) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
			sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
			version = sdf.format(new Date());
		} else {
			version = project.getVersion().trim();
		}
		return version;
	}

	private String composeInstallDir() {
		if (!isNullOrBlank(installDir)) {
			return installDir.endsWith("/") ? installDir + project.getArtifactId() : installDir + "/" + project.getArtifactId();
		}
		return "/home/" + unixUserId + "/" + project.getArtifactId();
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
		ignore.add(PREINST);
		ignore.add(POSTINST);
		ignore.add(PRERM);
		ignore.add(POSTRM);
	}

	private void validate() throws MojoExecutionException {
		if (!PACKAGE_NAME.matcher(project.getArtifactId()).matches()) {
			throw new MojoExecutionException("invalid package name: " + project.getArtifactId() + " supported: " + PACKAGE_NAME.pattern());
		}
		if (isNullOrBlank(unixUserId)) {
			throw new MojoExecutionException("unixUserId should be specified");
		}
		if (unixUserId.trim().length() > 8) {
			throw new MojoExecutionException("unixUserId lenght should be less than 8");
		}
		if (installDir != null && !installDir.startsWith("/")) {
			throw new MojoExecutionException("installDir must be absolute");
		}
		if (fileSets == null || fileSets.isEmpty()) {
			throw new MojoExecutionException("fileSets cannot be empty");
		}
		File debDir = new File(debBaseDir);
		if (!debDir.exists()) {
			throw new MojoExecutionException(".deb base directory doesnt exist: " + debBaseDir);
		}
		validateDescription();
		if (project.getInceptionYear() == null) {
			throw new MojoExecutionException("inceptionYear is required for copyright file");
		}
		validateLicense();
		validateDeveloper();
	}

	private void validateDescription() throws MojoExecutionException {
		if (project.getDescription() == null || project.getDescription().trim().length() == 0) {
			throw new MojoExecutionException("project description is mandatory");
		}
		// lintian considers these as ERROR
		if (project.getDescription().equals(project.getArtifactId())) {
			getLog().warn("description-is-pkg-name: " + project.getDescription());
		}
		if (project.getDescription().toLowerCase(Locale.UK).startsWith(project.getArtifactId().toLowerCase(Locale.UK))) {
			getLog().warn("description-starts-with-package-name: " + project.getDescription());
		}
	}

	private void validateLicense() throws MojoExecutionException {
		if (project.getLicenses() == null || project.getLicenses().isEmpty()) {
			throw new MojoExecutionException("licenses are required for copyright file. At least one should be specified");
		}
		LicenseName licenseName = LicenseName.valueOfShortName(project.getLicenses().get(0).getName());
		if (licenseName == null) {
			throw new MojoExecutionException("unsupported license name. Valid values are: " + LicenseName.getAllShortNames());
		}
	}

	private void validateDeveloper() throws MojoExecutionException {
		if (project.getDevelopers() == null || project.getDevelopers().isEmpty()) {
			throw new MojoExecutionException("project maintainer is mandatory. Please specify valid \"developers\" entry");
		}
		Developer dev = project.getDevelopers().get(0);
		if (dev == null) {
			throw new MojoExecutionException("project maintainer is mandatory. Please specify valid \"developers\" entry");
		}
		if (isNullOrBlank(dev.getName())) {
			throw new MojoExecutionException("project maintainer name is mandatory. Please fill valid developer name");
		}
		if (isNullOrBlank(dev.getEmail())) {
			throw new MojoExecutionException("project maintainer email is mandatory. Please specify valid developer email");
		}
		Matcher m = EMAIL.matcher(dev.getEmail());
		if (!m.matches()) {
			throw new MojoExecutionException("invalid project maintainer email: " + dev.getEmail());
		}
	}

	private void fillDataTar(Config config, ArFileOutputStream output) throws MojoExecutionException {
		try (TarArchiveOutputStreamExt tar = new TarArchiveOutputStreamExt(new GZIPOutputStream(new ArWrapper(output)))) {
			tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
			if (Boolean.TRUE.equals(javaServiceWrapper)) {
				byte[] daemonData = processTemplate(freemarkerConfig, config, "daemon.ftl");
				TarArchiveEntry initScript = new TarArchiveEntry("etc/init.d/" + project.getArtifactId());
				initScript.setSize(daemonData.length);
				initScript.setMode(040755);
				tar.writeEntry(initScript, daemonData);
			}
			setupCopyright(config, tar);
			// make path relative
			String packageBaseDir = config.getInstallDir().substring(1) + "/";
			fileSets.sort(MappingPathComparator.INSTANCE);
			for (Fileset curPath : fileSets) {
				// relative path is relative to the installDir
				if (curPath.getTarget().charAt(0) != '/') {
					curPath.setTarget(packageBaseDir + curPath.getTarget());
				} else {
					// make absolute path relative for the tar archive
					curPath.setTarget(curPath.getTarget().substring(1));
				}
				addRecursively(tar, curPath);
			}
		} catch (Exception e) {
			throw new MojoExecutionException("unable to create data tar", e);
		}
	}

	private void setupCopyright(Config config, TarArchiveOutputStreamExt tar) throws TemplateException, IOException {
		byte[] data = null;
		if (config.getCustomCopyRightFile() != null) {
			File copyRightFile = new File(config.getCustomCopyRightFile());
			if (copyRightFile.isFile()) {
				data = Files.readAllBytes(copyRightFile.toPath());
			}
		}
		if (data == null) {
			data = processTemplate(freemarkerConfig, config, "copyright.ftl");
		}
		long size = data.length;
		TarArchiveEntry copyrightEntry = new TarArchiveEntry("usr/share/doc/" + project.getArtifactId() + "/copyright");
		copyrightEntry.setSize(size);
		copyrightEntry.setMode(040644);
		tar.writeEntry(copyrightEntry, data);
	}

	private void addRecursively(TarArchiveOutputStreamExt tar, Fileset fileset) throws IOException {
		File sourceFile = new File(fileset.getSource());
		String targetFilename = fileset.getTarget();
		// skip well-known ignore directories
		if (ignore.contains(sourceFile.getName()) || sourceFile.getName().endsWith(".rrd") || sourceFile.getName().endsWith(".log")) {
			return;
		}
		if (sourceFile.isDirectory()) {
			File[] subFiles = sourceFile.listFiles();
			for (File curSubFile : subFiles) {
				Fileset curSubFileset = new Fileset(fileset.getSource() + "/" + curSubFile.getName(), fileset.getTarget() + "/" + curSubFile.getName());
				addRecursively(tar, curSubFileset);
			}
			return;
		}
		if (sourceFile.isFile()) {
			TarArchiveEntry curEntry = new TarArchiveEntry(targetFilename);
			if (sourceFile.canExecute()) {
				curEntry.setMode(040755);
			}
			curEntry.setSize(sourceFile.length());
			try (InputStream fis = new FileInputStream(sourceFile)) {
				tar.writeEntry(curEntry, fis);
			}
		}

	}

	private void fillControlTar(Config config, ArFileOutputStream output) throws MojoExecutionException {
		try (TarArchiveOutputStreamExt tar = new TarArchiveOutputStreamExt(new GZIPOutputStream(new ArWrapper(output)))) {
			tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

			byte[] controlData = processTemplate(freemarkerConfig, config, "control.ftl");
			TarArchiveEntry controlEntry = new TarArchiveEntry("control");
			controlEntry.setSize(controlData.length);
			tar.putArchiveEntry(controlEntry);
			tar.write(controlData);
			tar.closeArchiveEntry();

			byte[] preinstBaseData = processTemplate(PREINST, freemarkerConfig, config, combine("preinst.ftl", debBaseDir + File.separator + PREINST, false));
			long size = preinstBaseData.length;
			TarArchiveEntry preinstEntry = new TarArchiveEntry(PREINST);
			preinstEntry.setSize(size);
			preinstEntry.setMode(0755);
			tar.putArchiveEntry(preinstEntry);
			tar.write(preinstBaseData);
			tar.closeArchiveEntry();

			byte[] postinstBaseData = processTemplate(POSTINST, freemarkerConfig, config, combine("postinst.ftl", debBaseDir + File.separator + POSTINST, true));
			size = postinstBaseData.length;
			TarArchiveEntry postinstEntry = new TarArchiveEntry(POSTINST);
			postinstEntry.setSize(size);
			postinstEntry.setMode(0755);
			tar.putArchiveEntry(postinstEntry);
			tar.write(postinstBaseData);
			tar.closeArchiveEntry();

			byte[] prermBaseData = processTemplate(PRERM, freemarkerConfig, config, combine("prerm.ftl", debBaseDir + File.separator + PRERM, false));
			size = prermBaseData.length;
			TarArchiveEntry prermEntry = new TarArchiveEntry(PRERM);
			prermEntry.setSize(size);
			prermEntry.setMode(0755);
			tar.putArchiveEntry(prermEntry);
			tar.write(prermBaseData);
			tar.closeArchiveEntry();

			byte[] postrmBaseData = processTemplate(POSTRM, freemarkerConfig, config, combine("postrm.ftl", debBaseDir + File.separator + POSTRM, false));
			size = postrmBaseData.length;
			TarArchiveEntry postrmEntry = new TarArchiveEntry(POSTRM);
			postrmEntry.setSize(size);
			postrmEntry.setMode(0755);
			tar.putArchiveEntry(postrmEntry);
			tar.write(postrmBaseData);
			tar.closeArchiveEntry();

		} catch (Exception e) {
			throw new MojoExecutionException("unable to create control tar", e);
		}
	}

	private static String combine(String classpathResource, String file, boolean inverse) throws IOException {
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

	private static void appendUserScript(String file, StringBuilder builder) throws IOException {
		File f = new File(file);
		if (!f.exists()) {
			return;
		}
		try (BufferedReader r = new BufferedReader(new FileReader(file))) {
			String curLine;
			while ((curLine = r.readLine()) != null) {
				builder.append(curLine).append('\n');
			}
		}
	}

	private static void appendSystemScript(String classpathResource, StringBuilder builder) throws IOException {
		try (BufferedReader isr = new BufferedReader(new InputStreamReader(DebianPackageMojo.class.getClassLoader().getResourceAsStream(classpathResource), StandardCharsets.UTF_8))) {
			String curLine;
			while ((curLine = isr.readLine()) != null) {
				builder.append(curLine).append('\n');
			}
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
