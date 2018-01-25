#!/bin/sh
# postinst script for mica2
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

case "$1" in
  [1-2])

    # Create mica user if it doesn't exist.
    if ! id mica > /dev/null 2>&1 ; then
      adduser --system --home-dir /var/lib/mica2 --no-create-home mica
    fi

    # Mica2 file structure on Debian
    # /etc/mica2: configuration
    # /usr/share/mica2: executable
    # /var/lib/mica2: data runtime
    # /var/log: logs

    rm -f /usr/share/mica2
    new_release="$(ls -t /usr/share/ |grep mica2|head -1)"
    ln -s /usr/share/${new_release} /usr/share/mica2

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

    chown -R mica:adm /var/lib/mica2 /var/log/mica2 /etc/mica2 /tmp/mica2
    chmod -R 750      /var/lib/mica2 /var/log/mica2 /etc/mica2/ /tmp/mica2
    find /etc/mica2/ -type f | xargs chmod 640

    # if upgrading to 2.0, delete old log4j config
    if [ -f "/etc/mica2/log4j.properties" ]; then
      mv /etc/mica2/log4j.properties /etc/mica2/log4j.properties.old
    fi

    # auto start on reboot
    chkconfig --add mica2

    # start mica2
    echo "### You can start mica2 service by executing:"
    echo "sudo /etc/init.d/mica2 start"

  ;;

  *)
    echo "postinst called with unknown argument \`$1'" >&2
    exit 1
  ;;
esac

exit 0
