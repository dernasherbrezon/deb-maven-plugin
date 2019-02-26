if ! id ${config.user} > /dev/null 2>&1 ; then
		useradd -m ${config.user} --shell /bin/bash
fi

if [ ! -d /${config.installDir} ]; then
        mkdir /${config.installDir}
        chown ${config.user}:${config.group} /${config.installDir}/
fi

