#! /bin/sh

#
# Copyright (c) 1999, 2010 Tanuki Software, Ltd.
# http://www.tanukisoftware.com
# All rights reserved.
#
# This software is the proprietary information of Tanuki Software.
# You shall use it only in accordance with the terms of the
# license agreement you entered into with Tanuki Software.
# http://wrapper.tanukisoftware.com/doc/english/licenseOverview.html
#
# Java Service Wrapper sh script.  Suitable for starting and stopping
#  wrapped Java applications on UNIX platforms.
#

#-----------------------------------------------------------------------------
# These settings can be modified to fit the needs of your application
# Optimized for use with version 3.5.3 of the Wrapper.

# Application
APP_NAME="${config.artifactId}"
APP_LONG_NAME="${config.name}"

# Wrapper
WRAPPER_CMD="/usr/sbin/wrapper"
WRAPPER_CONF="${config.installDir}/etc/wrapper.app.conf"

# Priority at which to run the wrapper.  See "man nice" for valid priorities.
#  nice is only used if a priority is specified.
PRIORITY=

# Location of the pid file.
PIDDIR="${config.installDir}/log"

# If uncommented, causes the Wrapper to be shutdown using an anchor file.
#  When launched with the 'start' command, it will also ignore all INT and
#  TERM signals.
#IGNORE_SIGNALS=true

# Wrapper will start the JVM asynchronously. Your application may have some
#  initialization tasks and it may be desirable to wait a few seconds
#  before returning.  For example, to delay the invocation of following
#  startup scripts.  Setting WAIT_AFTER_STARTUP to a positive number will
#  cause the start command to delay for the indicated period of time 
#  (in seconds).
# 
WAIT_AFTER_STARTUP=0

# If set, wait for the wrapper to report that the daemon has started
WAIT_FOR_STARTED_STATUS=true
WAIT_FOR_STARTED_TIMEOUT=120

# If set, the status, start_msg and stop_msg commands will print out detailed
#   state information on the Wrapper and Java processes.
#DETAIL_STATUS=true

# If specified, the Wrapper will be run as the specified user.
# IMPORTANT - Make sure that the user has the required privileges to write
#  the PID file and wrapper.log files.  Failure to be able to write the log
#  file will cause the Wrapper to exit without any way to write out an error
#  message.
# NOTE - This will set the user which is used to run the Wrapper as well as
#  the JVM and is not useful in situations where a privileged resource or
#  port needs to be allocated prior to the user being changed.
RUN_AS_USER=${config.user}

# The following two lines are used by the chkconfig command. Change as is
#  appropriate for your application.  They should remain commented.
# chkconfig: 2345 20 80
# description: ${config.description}

# When installing on On Mac OSX platforms, the following domain will be used to
#  prefix the plist file name.
PLIST_DOMAIN=org.tanukisoftware.wrapper

# Initialization block for the install_initd and remove_initd scripts used by
#  SUSE linux distributions.
### BEGIN INIT INFO
# Provides: ${config.artifactId}
# Required-Start: $local_fs $remote_fs $network $syslog
# Should-Start: 
# Required-Stop:
# Default-Start: 2 3 4 5
# Default-Stop: 0 1 6
# Short-Description: ${config.name}
# Description: ${config.description}
### END INIT INFO

# Set run level to use when installing the application to start and stop on
#  system startup and shutdown.  It is important that the application always
#  be uninstalled before making any changes to the run levels.
# It is also possible to specify different run levels based on the individual
#  platform.  When doing so this script will look for defined run levels in
#  the following order:
#   1) "RUN_LEVEL_S_$DIST_OS" or "RUN_LEVEL_K_$DIST_OS", where "$DIST_OS" is
#      the value of DIST_OS.  "RUN_LEVEL_S_solaris=20" for example.
#   2) RUN_LEVEL_S or RUN_LEVEL_K, to specify specify start or stop run levels.
#   3) RUN_LEVEL, to specify a general run level.
RUN_LEVEL=20

# Do not modify anything beyond this point
#-----------------------------------------------------------------------------

if [ -f "/etc/default/$APP_NAME" ]; then
        . "/etc/default/$APP_NAME"
fi

# WRAPPER_PREINIT START
# WRAPPER_PREINIT END

. "/usr/share/wrapper/daemon.sh"