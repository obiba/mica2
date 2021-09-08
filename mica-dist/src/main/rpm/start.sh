#!/bin/bash

$JAVA $JAVA_ARGS -cp "${MICA_HOME}/conf:${MICA_DIST}/webapp/WEB-INF/classes:${MICA_DIST}/webapp/WEB-INF/lib/*" -DMICA_HOME=${MICA_HOME} -DMICA_DIST=${MICA_DIST} -DMICA_LOG=${MICA_LOG} org.obiba.mica.Application $MICA_ARGS
