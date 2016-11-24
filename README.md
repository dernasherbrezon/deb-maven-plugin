# deb-maven-plugin

maven plugin for .deb packaging

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
    <td>target file or directory in result .deb file. Please note that in .deb file absolute paths start without "/"</td>
    </tr>    
    
<tr>
    <td>filter</td>
    <td>true/false. Apply filter rules. Please note that source should contain text files.</td>
    </tr>    
    
  </tbody>
</table>
