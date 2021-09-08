#!/bin/sh

getent group adm >/dev/null || groupadd -r adm

getent passwd mica >/dev/null || \
	  useradd -r -g nobody -d /var/lib/mica2 -s /sbin/nologin -c "mica service user" mica
exit 0
