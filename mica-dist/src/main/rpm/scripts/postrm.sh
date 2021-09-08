#!/bin/sh
# postrm script for mica
#

set -e

# summary of how this script can be called:
#        * <postrm> `remove'
#        * <postrm> `purge'
#        * <old-postrm> `upgrade' <new-version>
#        * <new-postrm> `failed-upgrade' <old-version>
#        * <new-postrm> `abort-install'
#        * <new-postrm> `abort-install' <old-version>
#        * <new-postrm> `abort-upgrade' <old-version>
#        * <disappearer's-postrm> `disappear' <overwriter>
#          <overwriter-version>
# for details, see http://www.debian.org/doc/debian-policy/ or
# the debian-policy package

systemctl daemon-reload >/dev/null 2>&1 || :
case "$1" in
  0)
    userdel -f mica || true
    unlink /usr/share/mica2
    # Remove logs and data
    rm -rf /var/log/mica2 /etc/mica2 /usr/share/mica2*

    # Backup mica2 home
    timestamp="$(date '+%N')"
    echo "Backing up /var/lib/mica2 to /var/lib/mica2-backup-$timestamp"
    mv /var/lib/mica2 /var/lib/mica2-backup-$timestamp >/dev/null 2>&1
    chown -R root:root /var/lib/mica2-backup-$timestamp >/dev/null 2>&1
  ;;

  1)
    # Package upgrade, not removal
    find /usr/share/mica2-* -empty -type d -delete
    systemctl try-restart mica2.service >/dev/null 2>&1 || :
  ;;
esac

exit 0
