#!/usr/bin/env bash
# Checks custom Mica templates for the changes required by Mica 8.0, where templates are HTML-escaped automatically.
#
# Usage: check-templates.sh DIR    (e.g. $MICA_HOME/conf/templates)
#
# Prints file:line: LEVEL message, then a count. Exit code 1 when an ERROR is found.
#   ERROR  the template fails, or the page or its scripts break
#   WARN   the page shows wrong text (escaped twice or not decoded) or a script can break on some values
#   INFO   to review, works as before
# A line is checked as a whole: a value written over several lines is not seen.
set -u
dir=${1:?usage: check-templates.sh DIR}
[ -d "$dir" ] || { echo "not a directory: $dir" >&2; exit 2; }

# hidden folders (.git, editor history) are skipped
find "$dir" -name '.?*' -prune -o -name '*.ftl' -print0 | sort -z | xargs -0 awk '
function report(level, msg) {
  printf "%s:%d: %s %s: %s\n", FILENAME, FNR, level, msg, substr(trim($0), 1, 160)
  count[level]++
}
function trim(s) { sub(/^[ \t]+/, "", s); sub(/[ \t]+$/, "", s); return s }
# occurrences of a regular expression in a text
function occurrences(s, re) { return gsub(re, "", s) }
# the name of the tag opened at the start of the text
function tagName(s) { s = substr(s, 2); sub(/[^a-zA-Z0-9].*/, "", s); return s }
# values that come from Mica settings or are system names, printed raw in scripts on purpose
function configValue(v) {
  return v ~ /^\$\{(contextPath|assetsPath|\.lang|defaultLang|defaultSearchMode|mapName|title|isPrivate|[a-zA-Z]*Icon|[a-zA-Z]*Color)\}$/ ||
    v ~ /^\$\{(configJson|formConfig\.|postLogoutRedirectUri)/ || v ~ /\?join\(/ || v ~ /\?replace\("<", "\\\\u003c"\)/
}
# identifiers made by Mica (documents, requests, sets, languages): no quotes in them
function systemId(v) {
  return v ~ /^\$\{[a-zA-Z_.]*(\.id|\.parentId|Id|\.language)\}$/ || v ~ /^\$\{type\?lower_case\}$/
}
FNR == 1 { plain = 0; script = 0; comment = 0; noparse = 0; xtemplate = 0; messageMacro = 0; vueApp = 0; vuePre = 0 }
{
  line = $0
  # FreeMarker comments and noparse blocks are not output
  if (comment) { if (line ~ /-->/) { comment = 0; sub(/.*-->/, "", line) } else next }
  gsub(/<#--.*-->/, "", line)
  if (line ~ /<#--/) { comment = 1; sub(/<#--.*/, "", line) }
  gsub(/<#noparse>.*<\/#noparse>/, "", line)
  if (noparse) { if (line ~ /<\/#noparse>/) noparse = 0; next }
  if (line ~ /<#noparse>/ && line !~ /<\/#noparse>/) { noparse = 1; next }

  if (line ~ /<#outputformat[ \t]+"(plainText|JavaScript|JSON)"/) plain++
  if (line ~ /<script([ \t>]|$)/ && line !~ /<script[^>]*src=/) {
    script = 1
    # HTML templates for Vue: their content is HTML
    xtemplate = line ~ /<script[^>]*type="text\/(x-template|template|html)"/
  }
  inScript = script && !xtemplate

  # template errors
  if (line ~ /<#escape[ \t>]/) report("ERROR", "#escape is not allowed with auto-escaping, remove it with its #noescape")
  if (line ~ /<#ftl[^>]*output_format/ && line !~ /output_format="HTML"/) report("ERROR", "the <#ftl output_format> header turns auto-escaping off or changes it, remove it")
  if (!plain && line ~ /(\$\{[^}]*|<[#@][^>]*)\?html([^a-zA-Z_]|$)/) report("ERROR", "?html is an error, remove it (?html still works in a plainText block)")
  if (plain && line ~ /\?no_esc([^a-zA-Z_]|$)/) report("ERROR", "?no_esc is an error inside a plainText block, remove it")

  # scripts
  if (inScript && !plain && line ~ /\$\{/) report("ERROR", "value in a <script> outside <#outputformat \"plainText\">: JSON breaks, quotes show as &#39;")
  if (inScript && line ~ /\$\{/) {
    s = line
    while (match(s, /\$\{[^}]*\}/)) {
      v = substr(s, RSTART, RLENGTH); s = substr(s, RSTART + RLENGTH)
      if (v ~ /\?(js_string|c|json_string)\}$/ || configValue(v) || systemId(v)) continue
      if (v ~ /(localize(Raw)?\(|esc\()/) report("WARN", "localize()/esc() in a script no longer escape quotes, add ?js_string: " v)
      else report("INFO", "script value without ?js_string (or ?c for a number): " v)
    }
  }
  # JavaScript in HTML attributes: the HTML escaping is undone by the browser before the script runs
  s = line
  while (match(s, /[ \t](on[a-z]+|:[a-zA-Z.-]+|v-[a-z:.-]+|@[a-zA-Z.:-]+)="[^"]*(\$\{|<@message)[^"]*"/)) {
    a = substr(s, RSTART, RLENGTH); s = substr(s, RSTART + RLENGTH)
    if (a ~ /<@message/ && a ~ /'\''<@message/) report("WARN", "translation in a JavaScript string of an attribute: an apostrophe breaks it, use getMessage(...)?js_string")
    t = a; bad = 0
    while (match(t, /\$\{[^}]*\}/)) {
      v = substr(t, RSTART, RLENGTH); t = substr(t, RSTART + RLENGTH)
      if (v !~ /\?(js_string|c)\}$/ && !configValue(v) && !systemId(v)) bad = 1
    }
    if (bad) report("WARN", "value in JavaScript of an attribute without ?js_string")
  }

  # translations can hold HTML: printed escaped, their tags show
  if (line ~ /<#macro[ \t]+message[ \t>]/) messageMacro = 1
  if (messageMacro && !plain && line ~ /\$\{text\}/) report("WARN", "translation printed escaped in the message macro, its HTML shows as text: print it with ${text?no_esc}")
  if (!plain && line ~ /\$\{[^}]*getMessage\(/ && line !~ /getMessage\([^}]*\?no_esc/) report("WARN", "translation printed escaped, its HTML shows as text: add ?no_esc")
  if (messageMacro && line ~ /<\/#macro>/) messageMacro = 0

  # Vue apps mounted on the page compile its HTML: {{ }} in a value would run, HTML escaping keeps the braces.
  # The mount element and the v-pre elements are followed by counting their tag; values in attributes are not compiled.
  if (!vueApp && match(line, /<[a-zA-Z0-9]+[^>]*id="(query-vue-container|search-application|summary-statistics|cards|data-access-form|[a-zA-Z0-9_${}?.-]*files-app)"/)) {
    vueApp = 1; vueTag = tagName(substr(line, RSTART)); vueDepth = 0
  }
  preStart = 0
  if (vueApp && !vuePre && match(line, /<[a-zA-Z0-9]+[^>]*[ \t]v-pre[ \t>=\/]/)) {
    preTag = tagName(substr(line, RSTART)); rest = substr(line, RSTART)
    preDepth = occurrences(rest, "<" preTag "[ \t>]") - occurrences(rest, "</" preTag ">")
    vuePre = preDepth > 0; preStart = 1
  }
  if (vueApp && !vuePre && !inScript && line !~ /v-pre/) {
    s = line; gsub(/="[^"]*"/, "", s); bad = 0
    while (match(s, /\$\{[^}]*\}/)) {
      v = substr(s, RSTART, RLENGTH); s = substr(s, RSTART + RLENGTH)
      # numbers cannot hold braces
      if (v !~ /(\?c|\?size|[cC]ount)\}$/ && !configValue(v) && !systemId(v)) bad = 1
    }
    if (bad) report("WARN", "value in a Vue app without v-pre: {{ }} in it would run, print it in an element with v-pre")
  }
  if (vuePre && !preStart) {
    preDepth += occurrences(line, "<" preTag "[ \t>]") - occurrences(line, "</" preTag ">")
    if (preDepth <= 0) vuePre = 0
  }
  if (vueApp) {
    vueDepth += occurrences(line, "<" vueTag "[ \t>]") - occurrences(line, "</" vueTag ">")
    if (vueDepth <= 0) { vueApp = 0; vuePre = 0 }
  }

  # values that are not escaped as before
  if (line ~ /<template[ >]/ && line ~ /\$\{/ && line !~ /templateSafe\(/) report("WARN", "Markdown in <template> without templateSafe() is shown escaped")
  if (line ~ /class="[^"]*marked[^"]*"[^<]*\$\{/ && line !~ /templateSafe\(/) report("WARN", "Markdown in .marked without <template> and templateSafe() is shown escaped")
  if (line ~ /\$\{[^}]*fullName/ && line !~ /htmlDecode\([^)]*fullName/) report("WARN", "fullName is HTML-encoded by Mica, print it with htmlDecode()")
  if (line ~ /\$\{[^}]*attributes\[/ && line !~ /htmlDecode\(/ && line !~ /attributes\[[^]]*\]\.toString\(\)/ && line !~ /attributes\["realm"\]/) report("WARN", "profile attributes are HTML-encoded by Mica, print them with htmlDecode()")
  # same with a dot (user.attributes.email); variable and category attributes are localized texts, printed with localize()
  else if (line ~ /\$\{[^}]*attributes\.[a-zA-Z]/ && line !~ /htmlDecode\(/ && line !~ /localize(Raw)?\([^)]*attributes\./ && line !~ /attributes\.[a-zA-Z_]+\.toString\(\)/ && line !~ /attributes\.realm/) report("WARN", "profile attributes are HTML-encoded by Mica, print them with htmlDecode()")
  if (line ~ /\$\{[^}]*dar\.title/ && line !~ /htmlDecode\([^)]*dar\.title/) report("WARN", "the data access list titles are HTML-encoded, print them with htmlDecode()")
  if (!plain && line ~ /\$\{([^}]*[^a-zA-Z_.])?query(\.|\[)/ && line !~ /htmlDecode\([^)]*query(\.|\[)/) report("WARN", "the URL parameters of custom pages (query) are HTML-encoded, print them with htmlDecode()")
  if (line ~ /(^|[^a-zA-Z_.])esc\(/) report("INFO", "esc() is deprecated and returns the value unchanged, remove it")

  if (line ~ /<\/script>/) { script = 0; xtemplate = 0 }
  if (line ~ /<\/#outputformat>/ && plain > 0) plain--
}
END {
  printf "%d error(s), %d warning(s), %d to review\n", count["ERROR"], count["WARN"], count["INFO"]
  exit count["ERROR"] > 0
}'
