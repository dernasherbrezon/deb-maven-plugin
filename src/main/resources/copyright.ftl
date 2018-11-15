Format: https://www.debian.org/doc/packaging-manuals/copyright-format/1.0/
Upstream-Name: ${config.artifactId}
Upstream-Contact: ${config.maintainer}
<#if config.sourceUrl?has_content>
Source: ${config.sourceUrl}
</#if>

Files: *
Copyright: ${config.copyright}
License: ${config.licenseName.shortName}
 On Debian and Ubuntu systems, the complete text of the GNU General Public
 License can be found in `/usr/share/common-licenses/${config.licenseName.shortName}'