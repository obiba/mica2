# Upgrade notes

Actions to perform when upgrading a Mica server. Go through every version between the
one installed and the one being deployed.

## 7.0.0

Security release: 2FA sign-in feedback, open redirect closed, credentials kept out of
logs, XSS hardening (response headers, custom pages, Markdown, inline files) and
authorization added on two administration endpoints. Also: publication notifications,
Agate groups resolved through the roles mapping, and data access request deletion fixed.

### Before upgrading

1. **Agate first.** Upgrade Agate to 5.1.0 or later before Mica:
   - The sign-in page now reports a failure on the 2FA step as an invalid code, which is
     only correct once Agate verifies the password before asking for the code (Agate
     5.0.0). Against an older Agate, a wrong password entered on the 2FA step is reported
     as a wrong code.
   - Mica now emails editors and reviewers when a study, network, dataset, project or file
     is published or unpublished. The email templates (`<type>Published`, `filePublished`)
     are provided by Agate 5.1.0. Against an older Agate these emails are not sent;
     publication itself is not affected.
2. **Check for overridden templates.** The bundled templates below changed:

   ```sh
   cd $MICA_HOME/conf/templates
   ls signin.ftl libs/signin-scripts.ftl libs/scripts.ftl compare.ftl dataset.ftl variable.ftl \
      project.ftl libs/project.ftl libs/settings.ftl \
      data-access-form.ftl data-access-preliminary-form.ftl data-access-feasibility-form.ftl \
      data-access-amendment-form.ftl data-access-agreement-form.ftl libs/data-access-form.ftl \
      data-accesses.ftl libs/contact-scripts.ftl libs/head.ftl libs/aside-navbar.ftl \
      libs/navbar-menus-left.ftl
   ```

   If any exists, keep a copy of the current bundled version to compare against:
   `/usr/share/mica2/webapp/WEB-INF/classes/_templates` (Debian/RPM package and Docker image),
   or `webapp/WEB-INF/classes/_templates` in the install folder of a ZIP install.
3. **Check custom page names.** Custom pages (`$MICA_HOME/conf/templates/<name>.ftl` served
   at `/page/<name>`) are now only reachable when `<name>` is made of letters, digits, `_`
   and `-`, starts with a letter or digit, and is not the name of a bundled template.
   Rename any page that does not comply and update links to it.
4. **Check redirect links.** The `redirect` parameter of the sign-in, sign-up and `/check`
   pages now only accepts a path on the Mica server or a URL on the Agate server. Any
   link on another site that redirects users back to an absolute URL after sign-in
   (`?redirect=https://...`) must use a local path instead, otherwise the user lands on
   the home page.
5. **Check API clients.** If scripts or external tools call the following endpoints,
   update them:
   - Study import from a remote Mica (`/ws/draft/studies/import/...`): the remote
     credentials are now sent in the `X-Mica-Remote-Username` and
     `X-Mica-Remote-Password` request headers instead of the `username` and `password`
     query parameters. Make sure a reverse proxy in front of Mica lets these headers
     through.
   - `HEAD /ws/auth/session/{id}` now only answers 200 for the caller's own session (the
     one identified by its cookie), 404 for any other session ID.
   - `PUT /ws/config/i18n/custom/{locale}.json`, `PUT /ws/config/i18n/custom/import`
     and `/ws/logs` now require the `mica-administrator` role.
   - The Word export of the preliminary, feasibility, amendment and agreement forms
     (`/ws/data-access-request/{id}/.../_word`) now checks the view permission on the form
     itself, like the other endpoints of these forms. Users allowed to view one of these forms
     can now export it; this used to be refused (403) to anyone without an administrator or DAO
     role. The portal only offers this export to administrators and
     DAOs.
6. **Check the roles mapping.** Skip this step if the `roles` section of
   `$MICA_HOME/conf/application.yml` is not customised. Each `roles.<role>` entry lists the
   Agate groups that grant a Mica role. Until now it was only applied at sign-in: whenever
   Mica asked Agate for the members of a role, it used the role name as the group name. Mica
   now asks for the mapped groups instead. This applies to the data access officer,
   reviewer and editor emails, the list of users on the data access page, and the default
   contact and sign-up groups.

   For example, with `mica-data-access-officer: dao-mica-a`, the data access officer emails
   used to be sent to the Agate group `mica-data-access-officer` and are now sent to
   `dao-mica-a`. Only the groups that grant the role on their own receive them: with
   `a,b|c`, only the members of `c` do. Check that the mapped groups are the ones that should
   receive these emails. `mica-external-editor` can now be mapped too.

### Upgrade

Install the new version and restart as usual. No database migration runs at startup.

### After upgrading

1. **Overridden templates** (skip if none of the files above exists). Start from the 7.0
   version of each file and re-apply your customisations. In particular:
   - `libs/scripts.ftl` now loads DOMPurify and renders Markdown through
     `renderMarkdown()`. If your copy does not load
     `${assetsPath}/libs/node_modules/dompurify/dist/purify.min.js`, Markdown
     descriptions (studies, networks, datasets, popovers) are displayed as escaped text
     instead of formatted HTML.
   - `signin.ftl` and `libs/signin-scripts.ftl`: without the change, a wrong 2FA code
     sends the user back to the credentials form with the generic authentication
     failure instead of "Invalid or expired code".
   - `libs/contact-scripts.ftl`: after the contact form is sent, the user is now taken to
     `${contextPath}/contact-success`. A copy that still goes to `/page/contact-success`
     lands on a "not found" page, because `contact-success` is a bundled template and can
     no longer be opened as a custom page (see "Check custom page names").
   - `compare.ftl`, `variable.ftl` and `dataset.ftl`: escaping fixes, re-apply them in
     your copy.
   - `test.ftl` was removed from the bundle. Delete any copy of it.
   - `project.ftl`, `libs/project.ftl` and `libs/settings.ftl`: research projects now get
     the same published-side file browser as studies and networks (attachments become
     browseable from the portal), gated by a new `showProjectFiles` flag next to
     `showStudyFiles`/`showNetworkFiles`/`showDatasetFiles`. `project.ftl` also gains an
     include of the new `libs/project-scripts.ftl` (nothing to reconcile there, the file
     is new). If your copy of `project.ftl` or `libs/project.ftl` is not updated, the file
     browser stays absent from the project page, same as before this release.
   - `data-access-form.ftl`, `data-access-{preliminary,feasibility,amendment,agreement}-form.ftl`
     and `libs/data-access-form.ftl`: administrators and DAOs get a **Download** menu on data
     access forms offering the form (Word or PDF, as before) and, when the form has files
     attached, a new ZIP download of these files (macro `dataAccessDownloadButtons` in
     `libs/data-access-form.ftl`, used by the 4 sub-form templates; shown only when the new
     `hasFiles` page variable is true). Files that cannot be retrieved from the file store are
     left out of the ZIP and listed in a `MISSING.txt` entry. If your copies of the templates are
     not updated, the ZIP download is not offered in the UI (the underlying `/files/_download`
     endpoints still work regardless).
   - `data-accesses.ftl`: the **New data access request** button is now only shown to users
     allowed to create one (new `canAddDar` page variable). An old copy shows it to everyone;
     users without the permission get an error when they use it.
   - `libs/head.ftl`: AdminLTE 4.10 follows the operating system's dark mode. The bundled
     template turns that off (`data-lte-color-mode="off"`). Without it, visitors using dark
     mode see the pages in AdminLTE's dark theme, which custom styles may not be designed for.
   - `libs/aside-navbar.ftl` and `libs/navbar-menus-left.ftl`: small-screen fixes for
     AdminLTE 4 (header class renamed to `app-header`, the Data Access button text no longer
     wraps).
2. **Custom translations**: the messages `sign-in-otp-failed` and `files` were added, bundled
   in English and French. Add them to any other language you provide.
3. **Response headers.** Mica now sends `X-Content-Type-Options: nosniff`,
   `Referrer-Policy: strict-origin-when-cross-origin` and, when the request reaches Mica
   over TLS, `Strict-Transport-Security: max-age=31536000; includeSubDomains`. If your
   reverse proxy already sets any of these, remove the duplicate on one side. When TLS is
   terminated by the proxy, Mica does not send the HSTS header: keep setting it on the
   proxy.
4. **Inline files.** Files served from the file system (`/ws/draft/file-dl/...`,
   `/ws/file-dl/...`, attachments) are only displayed inline (`?inline=true`) for PDF,
   raster images (PNG, JPEG, GIF, WebP, BMP, TIFF), plain text, CSV and TSV. SVG, XML,
   HTML and any other type are always downloaded as attachments. Custom pages or
   descriptions that embed an uploaded SVG or HTML file inline must switch to a raster
   image or serve the file from the `assets` directory.
5. **New emails.** Editors and reviewers now receive an email when an entity or a file is
   published or unpublished, and the users allowed to read private comments on data access
   requests (e.g. a data access committee) now receive the private-comment emails. They use
   the existing per-type switches of the notification settings: turn them off there if
   these emails are not wanted.
6. **Check** with a user for whom 2FA is enforced or activated: a wrong password is refused
   right away from the credentials form; a wrong code keeps the 2FA step open and shows
   "Invalid or expired code". Also open a study page with a Markdown description and
   check it is rendered as formatted HTML.
7. **Recommended: clean up the data left by deleted entities.** Before 7.0, deleting a data
   access request, study, network, dataset or project left some of its data in the database:

   - data access requests: permissions, agreements and their form files, comments and
     collaborators;
   - studies, networks, datasets and projects: permissions, comments and, for individual
     studies and datasets, their files.

   A new entity that reuses the id of a deleted one inherits that data: permissions, comments,
   files and, for a data access request, collaborators. A pending invitation to a deleted
   request can then be accepted on the new one. 7.0 deletes all of it with the entity, but
   does not clean up what was deleted earlier.

   Two scripts remove it: `dar-orphan-cleanup.js` and `entity-orphan-cleanup.js`. They are in
   the `tools` folder of the distribution (`/usr/share/mica2/tools` for the Debian/RPM
   package and the Docker image, `tools` in the install folder of a ZIP install) and in the
   [repository](https://github.com/obiba/mica2/tree/7.0.0/mica-dist/src/main/tools). They only
   delete the data of ids that no longer exist, never touch existing entities, and change
   nothing unless `APPLY` is set. They can be run more than once. Logo files of deleted
   studies are not removed (they only take up storage).

   **Server install.** Add your connection options (e.g. `--uri`) if MongoDB requires them.
   For a ZIP install, replace `/usr/share/mica2` with the install folder.

   1. Back up the database:

      ```sh
      mongodump --db mica --out mica-backup
      ```

   2. Do a dry run of each script. It lists what would be deleted for each id and changes
      nothing:

      ```sh
      mongosh --quiet mica /usr/share/mica2/tools/dar-orphan-cleanup.js
      mongosh --quiet mica /usr/share/mica2/tools/entity-orphan-cleanup.js
      ```

   3. Stop Mica, then apply:

      ```sh
      mongosh --quiet mica --eval 'var APPLY = true' --file /usr/share/mica2/tools/dar-orphan-cleanup.js
      mongosh --quiet mica --eval 'var APPLY = true' --file /usr/share/mica2/tools/entity-orphan-cleanup.js
      ```

   4. Start Mica. Permissions are cached, so a restart is required. A new dry run should
      report nothing to delete.

   **Docker Compose.** Run from the folder of your `docker-compose.yml`, once the `mica`
   service runs the 7.0 image. The services are assumed to be named `mica` and `mongo`. The
   MongoDB image must provide `mongosh` (official `mongo` images 6.0 and later do). If MongoDB
   requires authentication, add the connection options (e.g. `--username`, `--password`,
   `--authenticationDatabase admin`) to the `mongodump`, `mongosh` and `mongorestore` commands.

   1. Copy the scripts from the Mica container into the MongoDB container:

      ```sh
      docker compose cp mica:/usr/share/mica2/tools/dar-orphan-cleanup.js .
      docker compose cp mica:/usr/share/mica2/tools/entity-orphan-cleanup.js .
      docker compose cp dar-orphan-cleanup.js mongo:/tmp/
      docker compose cp entity-orphan-cleanup.js mongo:/tmp/
      ```

   2. Back up the database:

      ```sh
      docker compose exec -T mongo mongodump --db mica --archive > mica-backup.archive
      ```

   3. Do a dry run of each script:

      ```sh
      docker compose exec mongo mongosh --quiet mica /tmp/dar-orphan-cleanup.js
      docker compose exec mongo mongosh --quiet mica /tmp/entity-orphan-cleanup.js
      ```

   4. Stop Mica, then apply:

      ```sh
      docker compose stop mica
      docker compose exec mongo mongosh --quiet mica --eval 'var APPLY = true' --file /tmp/dar-orphan-cleanup.js
      docker compose exec mongo mongosh --quiet mica --eval 'var APPLY = true' --file /tmp/entity-orphan-cleanup.js
      ```

   5. Start Mica. A new dry run should report nothing to delete.

      ```sh
      docker compose start mica
      ```

### Rolling back

Reinstall 6.3.x and restart. Mica itself changed no data. If you ran the cleanup scripts,
restore the backup taken before them: `mongorestore --drop mica-backup`, or with Docker
Compose `docker compose exec -T mongo mongorestore --drop --archive < mica-backup.archive`.
This also discards everything written to the database after the backup.
Restore the previous overridden templates and revert API clients to query-parameter
credentials for the study import.
