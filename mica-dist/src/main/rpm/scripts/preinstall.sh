#!/bin/sh

case "$1" in
  2)
    latest_version="$(ls -t /usr/share | grep mica2- | head -1| cut -d'-' -f2)"
    if [ ! -z "$latest_version" ] ; then
      latest_version_number="${latest_version//.}"
      if [ $latest_version_number -lt 321 ] ; then
        echo
        echo "WARNING: versions before 3.2.1 have an uninstall script error, please run the"
        echo "following script to safely remove the current version before installing a new"
        echo "version:"
        echo
        echo "https://download.obiba.org/tools/rpm/safely-remove-mica-package-before-3.2.1.sh.gz"
        echo
        exit 1
      fi
    fi
  ;;
esac

getent group adm >/dev/null || groupadd -r adm
getent passwd mica >/dev/null || \
    useradd -r -g adm -d /home/mica -s /sbin/nologin \
    -c "User for Mica Server" mica
exit 0
