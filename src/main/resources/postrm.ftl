<#if config.javaServiceWrapper>
if [ "$1" = "purge" ] ; then
        update-rc.d ${config.artifactId} remove >/dev/null || true
fi
</#if>

case "$1" in
        purge|remove)
                if [ -d "${config.installDir}" ]; then
                    rm -R "${config.installDir}"
                fi
        ;;
        upgrade|failed-upgrade|abort-install|abort-upgrade|disappear)
        ;;
        *)
                echo "$0 called with unknown argument \`$1'" >&2
                exit 1
        ;;
esac
