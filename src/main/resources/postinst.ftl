USER_HOME=`echo ~${config.user}`

case "$1" in
        configure)
        chown -R ${config.user}:${config.group} $USER_HOME/${config.artifactId}
        chmod -R 750  $USER_HOME/${config.artifactId}
;;
esac
if [ -x "/etc/init.d/${config.artifactId}" ]; then
        update-rc.d ${config.artifactId} defaults >/dev/null
        if [ -x "`which invoke-rc.d 2>/dev/null`" ]; then
                invoke-rc.d ${config.artifactId} start || true
        else
                /etc/init.d/${config.artifactId} start || true
        fi
fi
