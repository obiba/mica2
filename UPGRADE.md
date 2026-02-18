# Upgrade notes

Actions to perform when upgrading a Mica server. Go through every version between the
one installed and the one being deployed.

## 7.0.0

Security release: 2FA sign-in feedback, open redirect closed, credentials kept out of
logs, XSS hardening (response headers, custom pages, Markdown, inline files) and
authorization added on two administration endpoints.

### Before upgrading

1. **Agate first.** The sign-in page now reports a failure on the 2FA step as an invalid
   code, which is only correct once Agate verifies the password before asking for the
   code. Upgrade Agate to 5.0.0 or later before Mica; against an older Agate, a wrong
   password entered on the 2FA step is reported as a wrong code.
2. **Check for overridden templates.** The bundled templates below changed:

   ```sh
   cd $MICA_HOME/conf/templates
   ls signin.ftl libs/signin-scripts.ftl libs/scripts.ftl compare.ftl dataset.ftl variable.ftl \
      project.ftl libs/project.ftl libs/settings.ftl \
      data-access-form.ftl data-access-preliminary-form.ftl data-access-feasibility-form.ftl \
      data-access-amendment-form.ftl data-access-agreement-form.ftl libs/data-access-form.ftl \
      libs/data-access-form-scripts.ftl
   ```

   If any exists, keep a copy of the current bundled version
   (`$MICA_DIST/WEB-INF/classes/_templates`) to compare against.
   If your portal also serves its own copy of `assets/js/mica-data-access-form.js` (outside of
   `$MICA_HOME/conf/templates`, e.g. a themed/rebuilt webapp), keep a copy of that too.
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
   - `data-access-form.ftl`, `data-access-{preliminary,feasibility,amendment,agreement}-form.ftl`,
     `libs/data-access-form.ftl` and `libs/data-access-form-scripts.ftl`: administrators and DAOs
     get a **Download** menu on data access forms offering the form (Word or PDF, as before) and
     a new ZIP download of all the files attached to it (macro `dataAccessDownloadButtons` in
     `libs/data-access-form.ftl`, used by the 4 sub-form templates). The download is triggered by
     a new `downloadFiles()` function added to `FormController` in the bundled
     `assets/js/mica-data-access-form.js`, wired from the templates above via `ng-click`; it shows
     a warning toast instead of downloading when the form has no files. If your copies of the
     templates are not updated, the ZIP download is not offered in the UI (the underlying
     `/files/_download` endpoints still work regardless). If you serve your own copy of
     `mica-data-access-form.js` (rather than the bundled one), merge in the `downloadFiles()`
     function as well — with the templates updated but not the JS, the Files menu item does
     nothing when clicked, with no error shown.
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
5. **Check** with a user for whom 2FA is enforced or activated: a wrong password is refused
   right away from the credentials form; a wrong code keeps the 2FA step open and shows
   "Invalid or expired code". Also open a study page with a Markdown description and
   check it is rendered as formatted HTML.

### Rolling back

Reinstall 6.3.x and restart. No data changed. Restore the previous overridden templates
and revert API clients to query-parameter credentials for the study import.

