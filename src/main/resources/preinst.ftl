if ! id ${config.user} > /dev/null 2>&1 ; then
		useradd -m ${config.user} --shell /bin/bash
fi

USER_HOME=`echo ~${config.user}`

if [ ! -d $USER_HOME/${config.artifactId} ]; then
        mkdir $USER_HOME/${config.artifactId}
        chown ${config.user}:${config.group} $USER_HOME/${config.artifactId}/
fi
if [ ! -d $USER_HOME/data ]; then
        mkdir $USER_HOME/data
        chown ${config.user}:${config.group} $USER_HOME/data/
fi
if [ ! -d $USER_HOME/data/${config.artifactId} ]; then
        mkdir $USER_HOME/data/${config.artifactId}
        chown ${config.user}:${config.group} $USER_HOME/data/${config.artifactId}
fi
if [ ! -L $USER_HOME/${config.artifactId}/data ]; then
        ln -s $USER_HOME/data/${config.artifactId} $USER_HOME/${config.artifactId}/data
        chown ${config.user}:${config.group} $USER_HOME/${config.artifactId}/data
fi
