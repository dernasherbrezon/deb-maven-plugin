# deb-maven-plugin [![Build Status](https://travis-ci.org/dernasherbrezon/deb-maven-plugin.svg?branch=master)](https://travis-ci.org/dernasherbrezon/deb-maven-plugin) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=com.aerse.maven%3Adeb-maven-plugin&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.aerse.maven%3Adeb-maven-plugin)

maven plugin for .deb packaging. Control file generated from pom.xml. Required fields:

* description
* name
* developers. At least 1 developer with valid email and name. This email will be used for ```maintainer``` section.

# Custom installation scripts

.deb file supports installation scripts. See <a href="https://www.debian.org/doc/debian-policy/ch-maintainerscripts.html" target="blank">manual</a>. Plugin supports these scripts as well. They should be placed into ```./src/main/deb/```. Supported scripts:

* preinst
* postinst
* prerm
* postrm

# configuration

<table>
  <thead>
    <tr>
      <th>
        Parameter
      </th>
      <th>
        Filter property name
      </th>
      <th>
        Description
      </th>
    </tr>
  </thead>
  <tbody>
	<tr>
    <td>unixUserId</td>
    <td>${config.unixUserId}</td>
    <td>User name for application files permissions.</td>
    </tr>
    <tr>
    <td>unixGroupId</td>
    <td>${config.unixGroupId}</td>
    <td>User group name for application files permissions.</td>
    </tr>
    <tr>
    <td>installDir</td>
    <td></td>
    <td>The directory into which the artefact is going to be installed. This value is optional. `/home/unixUserId` will be used if omitted </td>
    </tr>
	  <tr>
    <td>osDependencies</td>
    <td></td>
    <td>List of ubuntu dependencies. Example: &lt;logrotate&gt;>=3.7.8&lt;/logrotate&gt;. This will add .deb package dependency logrotate with version >=3.7.8</td>
    </tr>
	  <tr>
    <td>javaServiceWrapper</td>
    <td></td>
    <td>Support tanuki service wrapper. This will create daemon configuration in `/etc/init.d/` directory and configure to use tanuki service wrapper</td>
    </tr>
	  <tr>
    <td>fileSets/fileSet*</td>
    <td></td>
    <td>File set to include into final .deb. Could be file or directory</td>
    </tr>
    <tr>
    <td>attachArtifact</td>
    <td></td>
    <td>true/false. Attach artifact to project</td>
    </tr>
    <tr>
    <td>customCopyRightFile</td>
    <td></td>
    <td>Optional. File containing custom copyright, Useful if your package contains files with several different licenses</td>
    </tr>
    <tr>
    <td>generateVersion</td>
    <td></td>
    <td>true/false. Default true. Auto generate .deb version from current time according to the pattern ```yyyyMMddHHmmss```. If false - version will be taken from the project's version</td>
    </tr>
  </tbody>
</table>

# fileSet

<table>
  <thead>
    <tr>
      <th>
        Parameter
      </th>
      <th>
        Description
      </th>
    </tr>
  </thead>
  <tbody>
	  <tr>
    <td>source</td>
    <td>source file or directory. Wildcards ARE NOT supported.</td>
    </tr>
    
<tr>
    <td>target</td>
    <td>target file or directory in result .deb file. Absolute paths will be placed in the corresponding absolute paths on the filesystem. Relative paths will be placed relative to the "installDir" parameter</td>
    </tr>    
    
  </tbody>
</table>

# Sample configuration

```xml
<plugin>
	<groupId>com.aerse.maven</groupId>
	<artifactId>deb-maven-plugin</artifactId>
	<version>1.9</version>
	<executions>
		<execution>
			<id>package</id>
			<phase>package</phase>
			<goals>
				<goal>package</goal>
			</goals>
		</execution>
	</executions>
	<configuration>
		<unixUserId>ubuntu</unixUserId>
		<unixGroupId>ubuntu</unixGroupId>
		<installDir>/usr/bin</installDir>
		<osDependencies>
			<openjdk-7-jdk></openjdk-7-jdk>
			<nginx></nginx>
			<logrotate>>=3.7.8</logrotate>
			<imagemagick></imagemagick>
			<maven></maven>
		</osDependencies>
		<customCopyRightFile>${project.basedir}/src/main/resources/customText.txt</customCopyRightFile>
		<javaServiceWrapper>true</javaServiceWrapper>
		<fileSets>
			<fileSet>
				<source>${basedir}/src/main/deb</source>
				<target>/</target>
			</fileSet>
		</fileSets>
	</configuration>
</plugin>
```
