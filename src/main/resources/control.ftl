Package: ${config.artifactId}
Version: ${config.version}
Section: ${config.section}
Priority: ${config.priority.getName()}
Architecture: ${config.arch}
Maintainer: ${config.maintainer}
<#if config.depends?has_content>
Depends: ${config.depends}
</#if>
<#if config.homepage?has_content>
Homepage: ${config.homepage}
</#if>
Description: ${config.name}
 ${config.description}
