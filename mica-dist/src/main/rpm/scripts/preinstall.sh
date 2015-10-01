#!/bin/sh

getent group adm >/dev/null || groupadd -r adm
getent passwd mica >/dev/null || \
    useradd -r -g adm -d /home/mica -s /sbin/nologin \
    -c "User for Mica Server" mica
exit 0
