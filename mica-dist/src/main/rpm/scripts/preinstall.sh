#!/bin/sh

case "$1" in
  1)
     useradd -r -g nobody -d /var/lib/mica2 -s /sbin/nologin -c "User for Mica Server" mica
  ;;

  2)
    # stop the service if running
    if service mica2 status > /dev/null; then
      if which service >/dev/null 2>&1; then
        service mica2 stop
      elif which invoke-rc.d >/dev/null 2>&1; then
        invoke-rc.d mica2 stop
      else
        /etc/init.d/mica2 stop
      fi
    fi

    usermod -g nobody mica -d /var/lib/mica

    latest_version="$(ls -t /usr/share | grep mica2- | head -1| cut -d'-' -f2)"
    if [ ! -z "$latest_version" ] ; then
      latest_version_number="${latest_version//.}"
      if [ $latest_version_number -lt 321 ] ; then
        echo
        echo "WARNING: versions before 3.2.1 have an uninstall script error, please run the"
        echo "following script to safely remove the current version before installing a new"
        echo "version:"
        echo
        echo "https://github.com/obiba/mica2/releases/download/3.2.1/safely-remove-mica-package-before-3.2.1.sh.gz"
        echo
        exit 1
      fi
    fi
  ;;
esac


exit 0
