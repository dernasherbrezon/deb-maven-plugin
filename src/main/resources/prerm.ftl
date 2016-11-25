if [ -x "/etc/init.d/${config.artifactId}" ]; then
        if [ -x "`which invoke-rc.d 2>/dev/null`" ]; then
                invoke-rc.d ${config.artifactId} stop || true
        else
                /etc/init.d/${config.artifactId} stop || true
        fi
fi
