# Upgrade notes

Actions to perform when upgrading a Mica server. Go through every version between the
one installed and the one being deployed.

## 6.4.0

### Before upgrading

1. **Agate first.** The sign-in page now reports a failure on the 2FA step as an invalid
   code, which is only correct once Agate verifies the password before asking for the
   code. Upgrade Agate to 4.3.0 or later before Mica; against an older Agate, a wrong
   password entered on the 2FA step is reported as a wrong code.
2. Check for overridden templates:

   ```sh
   ls $MICA_HOME/conf/templates/signin.ftl $MICA_HOME/conf/templates/libs/signin-scripts.ftl
   ```

   If either exists, keep a copy of the current bundled version
   (`$MICA_DIST/WEB-INF/classes/_templates`) to compare against.

### Upgrade

Install the new version and restart as usual. No database migration runs at startup.

### After upgrading

1. **Overridden sign-in templates** (skip if none of the files above exists). Start from the
   6.4 version of `signin.ftl` and `libs/signin-scripts.ftl` and re-apply your
   customisations. An overridden template keeps working as before, it only lacks the new
   invalid-code message: the user is sent back to the credentials form with the generic
   authentication failure.
2. **Custom translations**: the message `sign-in-otp-failed` was added, bundled in English
   and French. Add it to any other language you provide.
3. **Check** with a user for whom 2FA is enforced or activated: a wrong password is refused
   right away from the credentials form; a wrong code keeps the 2FA step open and shows
   "Invalid or expired code".

### Rolling back

Reinstall 6.3.x and restart. No data changed.
