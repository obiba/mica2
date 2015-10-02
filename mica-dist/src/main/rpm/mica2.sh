#!/bin/bash

getPidFile() {
   while getopts ":p:" opt; do
     case $opt in
       p)
         echo $OPTARG
         return 0
         ;;
     esac
   done

   return 1
}

# OS specific support.
cygwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
esac

if [ -z "$JAVA_OPTS" ]
then
  if [ ! -z "$JAVA_ARGS" ]
  then
    JAVA_OPTS=$JAVA_ARGS
  else
    # Set default JAVA_OPTS
    JAVA_OPTS="-Xmx2G -XX:MaxPermSize=128M"
  fi

  export JAVA_OPTS
fi

# The directory containing the mica2 shell script
MICA_BIN_DIR=`dirname $0`
# resolve links - $0 may be a softlink
MICA_DIST=$(readlink -f $MICA_BIN_DIR/../webapp)

export MICA_DIST

echo "JAVA_OPTS=$JAVA_OPTS"
echo "MICA_HOME=$MICA_HOME"
echo "MICA_DIST=$MICA_DIST"
echo "MICA_LOG=$MICA_LOG"

if [ -z "$MICA_HOME" ]
then
  echo "MICA_HOME not set."
  exit 2;
fi

if $cygwin; then
  # For Cygwin, ensure paths are in UNIX format before anything is touched
  [ -n "$MICA_DIST" ] && MICA_BIN=`cygpath --unix "$MICA_DIST"`
  [ -n "$MICA_HOME" ] && MICA_HOME=`cygpath --unix "$MICA_HOME"`

  # For Cygwin, switch paths to Windows format before running java
  export MICA_DIST=`cygpath --absolute --windows "$MICA_DIST"`
  export MICA_HOME=`cygpath --absolute --windows "$MICA_HOME"`
fi

# Java 6 supports wildcard classpath entries
# http://download.oracle.com/javase/6/docs/technotes/tools/solaris/classpath.html
CLASSPATH=$MICA_HOME/conf:$MICA_DIST/WEB-INF/classes:$MICA_DIST/WEB-INF/lib/*
if $cygwin; then
  CLASSPATH=$MICA_HOME/conf;$MICA_DIST/WEB-INF/classes;$MICA_DIST/WEB-INF/lib/*
fi

[ -e "$MICA_HOME/logs" ] || mkdir "$MICA_HOME/logs"

JAVA_DEBUG=-agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n

# Add $JAVA_DEBUG to this line to enable remote JVM debugging (for developers)
exec java $JAVA_OPTS -cp "$CLASSPATH" -DMICA_HOME="${MICA_HOME}" \
  -DMICA_DIST=${MICA_DIST} -DMICA_LOG=${MICA_LOG}  org.obiba.mica.Application "$@" >$MICA_LOG/stdout.log 2>&1 &

# On CentOS 'daemon' function does not initialize the pidfile
pidfile=$(getPidFile $@)

if [ ! -z "$pidfile" ]; then
  echo $! > $pidfile
fi
