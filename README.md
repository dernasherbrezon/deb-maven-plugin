# deb-maven-plugin
maven plugin for .deb packaging

maven plugin for .deb packaging

Configuration
-------------

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

  </tbody>
</table>
