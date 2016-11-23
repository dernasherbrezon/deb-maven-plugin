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
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TimeZone;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.model.Developer;
import org.apache.maven.model.Resource;
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

	private static final Pattern installDirPattern = Pattern.compile("\\$\\{install\\.dir\\}");
	/**
	 * The maven project.
	 * 
	 * @parameter expression="${project}"
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
	private Properties additionalPaths;

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
	 * @parameter
	 */
	private String wrapperConfig;

	/**
	 * @parameter
	 */
	private String groupIDToExtract;

	/**
	 * @parameter default-value=true;
	 */
	private Boolean attachArtifact;
	
	/**
	 * @parameter
	 */
	private String groupIDToInclude;

	/**
	 * Location of the local repository.
	 * 
	 * @parameter expression="${localRepository}"
	 * @readonly
	 * @required
	 */
	private ArtifactRepository local;

	private String maintainer;
	private final static String BASE_DIR = "./src/main/deb";
	private final static Pattern email = Pattern.compile("^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");
	private String installDir;
	private List<String> dirsAdded = new ArrayList<String>();

	@Override
	public void execute() throws MojoExecutionException {

		validate();
		fillDefaults();

		Configuration freemarkerConfig = new Configuration();
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
		config.setMaintainer(maintainer);
		config.setName(project.getName());
		config.setDescription(project.getDescription());
		config.setDepends(formatDependencies(osDependencies));
		config.setWrapperConfig(wrapperConfig);

		ArFileOutputStream aros = null;
		try {
			File debFile = new File(project.getBuild().getDirectory() + File.separator + project.getArtifactId() + "-" + config.getVersion() + ".deb");
			getLog().info("Building deb: " + debFile.getAbsolutePath());
			aros = new ArFileOutputStream(debFile.getAbsolutePath());
			aros.putNextEntry(createEntry("debian-binary"));
			aros.write("2.0\n".getBytes(StandardCharsets.US_ASCII));
			aros.closeEntry();
			aros.putNextEntry(createEntry("control.tar.gz"));
			fillControlTar(config, freemarkerConfig, aros);
			aros.closeEntry();
			aros.putNextEntry(createEntry("data.tar.gz"));
			fillDataTar(config, freemarkerConfig, aros);
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
		Developer dev = project.getDevelopers().get(0);
		maintainer = dev.getName() + " <" + dev.getEmail() + ">";
		installDir = "/home/" + unixUserId + "/" + project.getArtifactId();
		if (wrapperConfig != null && wrapperConfig.trim().length() != 0 && !osDependencies.containsKey("service-wrapper")) {
			osDependencies.put("service-wrapper", null);
		}
	}

	private void validate() throws MojoExecutionException {
		if (unixUserId == null || unixUserId.trim().length() == 0) {
			throw new MojoExecutionException("unixUserId should be specified");
		}
		if (unixUserId.trim().length() > 8) {
			throw new MojoExecutionException("unixUserId lenght should be 8. I don't know the maximum");
		}
		File debDir = new File(BASE_DIR);
		if (!debDir.exists()) {
			throw new MojoExecutionException(".deb base directory doesnt exist: " + BASE_DIR);
		}
		boolean hasWrapperConfig = wrapperConfig != null && wrapperConfig.trim().length() != 0;
		if (hasWrapperConfig) {
			List<File> dirsToCheck = new ArrayList<File>();
			dirsToCheck.add(new File(debDir, "etc"));
			for (Resource cur : project.getResources()) {
				dirsToCheck.add(new File(cur.getDirectory()));
			}
			boolean found = false;
			for (File curDir : dirsToCheck) {
				File curWrapper = new File(curDir, wrapperConfig);
				if (curWrapper.exists()) {
					found = true;
					break;
				}
			}
			if (!found) {
				StringBuilder paths = new StringBuilder();
				for (File cur : dirsToCheck) {
					paths.append(cur.getAbsolutePath()).append(" ");
				}
				throw new MojoExecutionException("cannot find wrapper config: " + wrapperConfig + " in paths: " + paths.toString());
			}
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

	private void fillDataTar(Config config, Configuration freemarkerConfig, ArFileOutputStream output) throws MojoExecutionException {
		TarArchiveOutputStream tar = null;
		try {
			tar = new TarArchiveOutputStream(new GZIPOutputStream(new ArWrapper(output)));
			tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
			if (wrapperConfig != null && wrapperConfig.trim().length() != 0) {
				byte[] daemonData = processTemplate(freemarkerConfig, config, "daemon.ftl");
				TarArchiveEntry initScript = new TarArchiveEntry("etc/init.d/" + project.getArtifactId());
				initScript.setSize(daemonData.length);
				initScript.setMode(040755);
				tar.putArchiveEntry(initScript);
				tar.write(daemonData);
				tar.closeArchiveEntry();
			}
			File[] dirs = new File(BASE_DIR).listFiles();
			String packageBaseDir = "home/" + unixUserId + "/" + project.getArtifactId() + "/";
			String resourcesBaseDir = "home/" + unixUserId + "/" + project.getArtifactId() + "/etc/";
			String binBaseDir = "home/" + unixUserId + "/" + project.getArtifactId() + "/bin/";
			for (File curFile : dirs) {
				if (!curFile.isDirectory()) {
					continue;
				}
				addRecursively(tar, curFile, packageBaseDir + curFile.getName(), resourcesBaseDir, binBaseDir, false);
			}
			if (additionalPaths != null && !additionalPaths.isEmpty()) {
				List<MappingPath> paths = new ArrayList<MappingPath>();
				for (Entry<Object, Object> curEntry : additionalPaths.entrySet()) {
					File curDir = new File((String) curEntry.getKey());
					if (!curDir.isDirectory()) {
						continue;
					}
					paths.add(new MappingPath((String) curEntry.getKey(), (String) curEntry.getValue()));
				}
				Collections.sort(paths, new MappingPathComparator());
				for (MappingPath curPath : paths) {
					addRecursively(tar, new File(curPath.getSourcePath()), packageBaseDir + curPath.getTargetPath(), resourcesBaseDir, binBaseDir, false);
				}
			}
			ArtifactFilter filter = new ScopeArtifactFilter(Artifact.SCOPE_COMPILE);
			project.setArtifactFilter(filter);
			List<Artifact> libs = new ArrayList<Artifact>();
			if (project.getArtifact() != null && project.getArtifact().getFile() != null && project.getArtifact().getType().equals("jar")) {
				libs.add(project.getArtifact());
			}
			for (Object cur : project.getArtifacts()) {
				Artifact curArtifact = (Artifact) cur;
				if (curArtifact.getScope().equals(Artifact.SCOPE_SYSTEM)) {
					continue;
				}
				libs.add(curArtifact);
			}

			if (!libs.isEmpty()) {
				writeDirectory(tar, packageBaseDir + "lib/");
			}

			for (Artifact cur : libs) {
				boolean extract;
				if (groupIDToExtract != null && groupIDToExtract.equals(cur.getGroupId())) {
					extract = true;
				} else {
					extract = false;
				}
				
				File fileToOutput;
				if (cur.getFile() != null) {
					fileToOutput = cur.getFile();
				} else {
					String path = local.pathOf(cur);
					URL curPath = new URL(local.getUrl() + path);
					fileToOutput = new File(curPath.toURI());
				}
				addRecursively(tar, fileToOutput, packageBaseDir + "lib/" + fileToOutput.getName(), resourcesBaseDir, binBaseDir, extract);
			}
		} catch (Exception e) {
			throw new MojoExecutionException("unable to create data tar", e);
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

	private void addRecursively(TarArchiveOutputStream tar, File curFile, String filename, String resourcesBaseDir, String binBaseDir, boolean extractToEtc) throws MojoExecutionException {
		if (curFile.isDirectory()) {
			if (curFile.getName().equals(".svn")) {
				return;
			}
			filename += "/";
		} else {
			if (curFile.getName().endsWith(".rrd") || curFile.getName().endsWith(".log")) {
				return;
			}
			if (extractToEtc) {
				extractSourceFilesToEtc(tar, curFile, resourcesBaseDir, binBaseDir);
			}
		}
		if (!dirsAdded.contains(filename)) {
			FileInputStream fis = null;
			try {
				TarArchiveEntry curEntry = new TarArchiveEntry(filename);
				if (!curFile.isDirectory()) {
					if (curFile.getName().endsWith(".sh")) {
						String data = read(curFile);
						Matcher m = installDirPattern.matcher(data);
						if (m.find()) {
							data = m.replaceAll(installDir);
						}
						byte[] bytes = data.getBytes();
						curEntry.setSize(bytes.length);
						tar.putArchiveEntry(curEntry);
						tar.write(bytes);
					} else {
						curEntry.setSize(curFile.length());
						tar.putArchiveEntry(curEntry);
						IOUtils.copy(new FileInputStream(curFile), tar);
					}
				} else {
					dirsAdded.add(filename);
					tar.putArchiveEntry(curEntry);
				}
				tar.closeArchiveEntry();
			} catch (Exception e) {
				throw new MojoExecutionException("unable to add data file: " + curFile.getAbsolutePath(), e);
			} finally {
				if (fis != null) {
					try {
						fis.close();
					} catch (IOException e) {
						throw new MojoExecutionException("unable to close data file: " + curFile.getAbsolutePath(), e);
					}
				}
			}
		}
		if (curFile.isDirectory()) {
			File[] subFiles = curFile.listFiles();
			for (File curSubFile : subFiles) {
				addRecursively(tar, curSubFile, filename + curSubFile.getName(), resourcesBaseDir, binBaseDir, extractToEtc);
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

	private static String read(File f) throws Exception {
		BufferedReader r = null;
		StringBuilder b = new StringBuilder();
		try {
			r = new BufferedReader(new FileReader(f));
			String curLine = null;
			while ((curLine = r.readLine()) != null) {
				b.append(curLine).append("\n");
			}
		} finally {
			if (r != null) {
				try {
					r.close();
				} catch (IOException e) {
					throw new MojoExecutionException("unable to close data file: " + f.getAbsolutePath(), e);
				}
			}
		}
		return b.toString();
	}

	private void extractSourceFilesToEtc(TarArchiveOutputStream tar, File jarFile, String resourcesBaseDir, String binBaseDir) throws MojoExecutionException {
		JarInputStream jis = null;
		try {
			jis = new JarInputStream(new FileInputStream(jarFile));
			JarEntry curEntry = null;
			while ((curEntry = jis.getNextJarEntry()) != null) {
				TarArchiveEntry curTarEntry = null;
				if (!curEntry.getName().endsWith(".class") && !curEntry.getName().endsWith("/") && !curEntry.getName().startsWith("META-INF")) {
					List<String> dirs = getDirs(curEntry.getName());
					for (String curDir : dirs) {
						if (dirsAdded.contains(curDir)) {
							continue;
						}
						writeDirectory(tar, resourcesBaseDir + curDir);
						dirsAdded.add(curDir);
					}
					curTarEntry = new TarArchiveEntry(resourcesBaseDir + curEntry.getName());
					curTarEntry.setSize(curEntry.getSize());
				}
				if (curTarEntry == null) {
					continue;
				}
				tar.putArchiveEntry(curTarEntry);
				//copy batch
				int curByte = -1;
				while ((curByte = jis.read()) != -1) {
					tar.write(curByte);
				}
				tar.closeArchiveEntry();
			}
		} catch (Exception e) {
			throw new MojoExecutionException("unable to extract resources to \"./etc\" folder from .jar: " + jarFile.getAbsolutePath(), e);
		} finally {
			if (jis != null) {
				try {
					jis.close();
				} catch (IOException e) {
					getLog().error("unable to close .jar while extracting resources", e);
				}
			}
		}
	}

	private static List<String> getDirs(String entryName) {
		List<String> result = new ArrayList<String>();
		int curIndex = -1;
		while ((curIndex = entryName.indexOf('/', curIndex + 1)) != -1) {
			result.add(entryName.substring(0, curIndex));
		}
		return result;
	}

	private void fillControlTar(Config config, Configuration freemarkerConfig, ArFileOutputStream output) throws MojoExecutionException {
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
			
			byte[] preinstBaseData = processTemplate(freemarkerConfig, config, "preinst.ftl");
			byte[] additionalPreinst = readControlFile(BASE_DIR + File.separator + "preinst", installDir);
			long size = preinstBaseData.length;
			if( additionalPreinst != null ) {
				size += additionalPreinst.length;
			}
			TarArchiveEntry preinstEntry = new TarArchiveEntry("./preinst");
			preinstEntry.setSize(size);
			tar.putArchiveEntry(preinstEntry);
			tar.write(preinstBaseData);
			if (additionalPreinst != null) {
				tar.write(additionalPreinst);
			}
			tar.closeArchiveEntry();
			
			byte[] postinstBaseData = processTemplate(freemarkerConfig, config, "postinst.ftl");
			byte[] additionalPostinst = readControlFile(BASE_DIR + File.separator + "postinst", installDir);
			size = postinstBaseData.length;
			if( additionalPostinst != null ) {
				size += additionalPostinst.length;
			}
			TarArchiveEntry postinstEntry = new TarArchiveEntry("./postinst");
			postinstEntry.setSize(size);
			tar.putArchiveEntry(postinstEntry);
			tar.write(postinstBaseData);
			if (additionalPostinst != null) {
				tar.write(additionalPostinst);
			}
			tar.closeArchiveEntry();
			
			byte[] prermBaseData = processTemplate(freemarkerConfig, config, "prerm.ftl");
			byte[] additionalPrerm = readControlFile(BASE_DIR + File.separator + "prerm", installDir);
			size = prermBaseData.length;
			if( additionalPrerm != null ) {
				size += additionalPrerm.length;
			}
			TarArchiveEntry prermEntry = new TarArchiveEntry("./prerm");
			prermEntry.setSize(size);
			tar.putArchiveEntry(prermEntry);
			tar.write(prermBaseData);
			if (additionalPrerm != null) {
				tar.write(additionalPrerm);
			}
			tar.closeArchiveEntry();
			
			byte[] postrmBaseData = processTemplate(freemarkerConfig, config, "postrm.ftl");
			byte[] additionalPostrm = readControlFile(BASE_DIR + File.separator + "postrm", installDir);
			size = postrmBaseData.length;
			if( additionalPostrm != null ) {
				size += additionalPostrm.length;
			}
			TarArchiveEntry postrmEntry = new TarArchiveEntry("./postrm");
			postrmEntry.setSize(size);
			tar.putArchiveEntry(postrmEntry);
			tar.write(postrmBaseData);
			if (additionalPostrm != null) {
				tar.write(additionalPostrm);
			}
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
		StringWriter w = new StringWriter();
		Map<String, Object> data = new HashMap<>();
		data.put("config", config);
		fTemplate.process(data, w);
		byte[] postinstBaseData = w.toString().getBytes(StandardCharsets.UTF_8);
		return postinstBaseData;
	}

	private static byte[] readControlFile(String filename, String installDir) throws MojoExecutionException {
		File fileToRead = new File(filename);
		if (!fileToRead.exists()) {
			return null;
		}
		StringBuilder result = new StringBuilder();
		String curLine = null;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(fileToRead));
			while ((curLine = reader.readLine()) != null) {
				Matcher m = installDirPattern.matcher(curLine);
				if (m.find()) {
					curLine = m.replaceAll(installDir);
				}
				result.append(curLine).append("\n");
			}
		} catch (Exception e) {
			throw new MojoExecutionException("unable to read control file: " + filename, e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					throw new MojoExecutionException("unable to close control file: " + filename, e);
				}
			}
		}
		return result.toString().getBytes();
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
