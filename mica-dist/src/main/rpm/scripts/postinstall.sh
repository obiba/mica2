#!/bin/sh
# postinst script for mica
#

set -e

# summary of how this script can be called:
#        * <postinst> `configure' <most-recently-configured-version>
#        * <old-postinst> `abort-upgrade' <new version>
#        * <conflictor's-postinst> `abort-remove' `in-favour' <package>
#          <new-version>
#        * <postinst> `abort-remove'
#        * <deconfigured's-postinst> `abort-deconfigure' `in-favour'
#          <failed-install-package> <version> `removing'
#          <conflicting-package> <version>
# for details, see http://www.debian.org/doc/debian-policy/ or
# the debian-policy package

NAME=mica2

[ -r /etc/default/$NAME ] && . /etc/default/$NAME

# Mica2 file structure on Debian
# /etc/mica2: configuration
# /usr/share/mica2: executable
# /var/lib/mica2: data runtime
# /var/log: logs

rm -f /usr/share/mica2
new_release="$(ls -t /usr/share/ |grep mica2|head -1)"
ln -s /usr/share/${new_release} /usr/share/mica2

if [ ! -e /var/lib/mica2/data ] ; then
  mkdir /var/lib/mica2/data
fi

if [ ! -e /var/lib/mica2/work ] ; then
  mkdir /var/lib/mica2/work
fi

if [ ! -e /var/lib/mica2/plugins ] ; then
  mkdir /var/lib/mica2/plugins
fi

if [ ! -e /var/lib/mica2/logs ] ; then
  mkdir /var/lib/mica2/logs
fi

if [ ! -e /var/log/mica2 ] ; then
  mkdir /var/log/mica2
fi

if [ ! -e /var/lib/mica2/conf ] ; then
  ln -s /etc/mica2 /var/lib/mica2/conf
fi

# Some deprecated configuration files
if [ -e /var/lib/mica2/conf/mica-categories.yml ] ; then
  mv /var/lib/mica2/conf/mica-categories.yml.old
fi

if [ -e /var/lib/mica2/conf/mica-aggregations.yml ] ; then
  mv /var/lib/mica2/conf/mica-aggregations.yml.old
fi

# Upgrade application.yml if necessary
if grep -q "profiles:" /etc/mica2/application.yml
  then
    cp /etc/mica2/application.yml /etc/mica2/application.yml.5.x
    cat /etc/mica2/application.yml.5.x | grep -v "profiles:" > /etc/mica2/application.yml
fi
if [ -f /etc/mica2/application.yml ] && [ ! -f /etc/mica2/application-prod.yml ]
  then
  mv -f /etc/mica2/application.yml /etc/mica2/application-prod.yml
fi

chown -R mica:adm /var/lib/mica2 /var/log/mica2 /etc/mica2
chmod -R 750      /var/lib/mica2 /var/log/mica2 /etc/mica2
find /etc/mica2/ -type f -print0 | xargs -0 chmod 640

# if upgrading to 2.0, delete old log4j config
if [ -f "/etc/mica2/log4j.properties" ]; then
  mv /etc/mica2/log4j.properties /etc/mica2/log4j.properties.old
fi

exit 0
