
case "$1" in
        configure)
        chown -R ${config.user}:${config.group} "${config.installDir}"
;;
esac
<#if config.javaServiceWrapper>
if [ -x "/etc/init.d/${config.artifactId}" ]; then
        update-rc.d ${config.artifactId} defaults >/dev/null
        if [ -x "`which invoke-rc.d 2>/dev/null`" ]; then
                invoke-rc.d ${config.artifactId} start || true
        else
                /etc/init.d/${config.artifactId} start || true
        fi
fi
</#if>
