Package: ${config.artifactId}
Version: ${config.version}
Section: ${config.section}
Priority: ${config.priority}
Architecture: ${config.arch}
Maintainer: ${config.maintainer}
<#if config.depends?has_content>
Depends: ${config.depends}
</#if>
Description: ${config.name}
 ${config.description}
