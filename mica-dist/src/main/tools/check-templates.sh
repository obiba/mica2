#!/usr/bin/env bash
# Checks custom Mica templates for the changes required by Mica 8.0, where templates are HTML-escaped automatically.
#
# Usage: check-templates.sh DIR    (e.g. $MICA_HOME/conf/templates)
#
# Prints file:line: LEVEL message, then a count. Exit code 1 when an ERROR is found.
#   ERROR  the template fails, the page or its scripts break, or user input can inject HTML or JavaScript
#   WARN   the page shows wrong text (escaped twice or not decoded) or a script can break on some values
#   INFO   to review, works as before
# A line is checked as a whole: a value written over several lines is not seen, nor a value made raw in an assignment
# (<#assign t = x?no_esc>), nor a value of an included file printed inside a Vue app mounted in another file.
set -u
dir=${1:?usage: check-templates.sh DIR}
[ -d "$dir" ] || { echo "not a directory: $dir" >&2; exit 2; }

# hidden folders (.git, editor history) below DIR are skipped, DIR itself can be hidden (-mindepth 1); one awk run for
# all the files, for a single count and exit code; a read loop, not mapfile -d: bash 3.2 on macOS
files=()
while IFS= read -r -d '' f; do files+=("$f"); done < <(find "$dir" -mindepth 1 -name '.?*' -prune -o -name '*.ftl' -print0 | sort -z)
[ ${#files[@]} -gt 0 ] || { echo "no .ftl file in $dir"; exit 0; }
awk '
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
# identifiers made by Mica (documents, requests, sets, languages): no quotes in them; a variable id holds its name
function systemId(v) {
  return (v ~ /^\$\{[a-zA-Z_.]*(\.id|\.parentId|\.datasetId|\.language)\}$/ && v !~ /[vV]ariable\.id\}$/) ||
    v ~ /^\$\{(dceId|popId|type\?lower_case)\}$/
}
# user input that Mica 7 sent HTML-encoded: user names and profile attributes, data access list titles, URL parameters
# of custom pages; variable and category attributes are localized texts, printed with localize()
function userValue(v) {
  if (v ~ /localize(Raw)?\([^)]*attributes\./ || v ~ /attributes(\[[^]]*\]|\.[a-zA-Z_]+)\.toString\(\)/) return 0
  return v ~ /fullName/ || v ~ /attributes(\[|\.[a-zA-Z])/ || v ~ /dar\.title/ || v ~ /(^\$\{|[^a-zA-Z_.])query(\.|\[)/
}
# a custom libs/head.ftl replaces the built-in one: the built-in templates call the messageJs() and messageAttr() it defines
# (called once the file is read: on the first line of the next one, FILENAME is already that one)
function checkHead(file) {
  if (file ~ /(^|\/)libs\/head\.ftl$/ && !(messageJsDefined && messageAttrDefined)) {
    printf "%s:1: ERROR the built-in templates call messageJs() and messageAttr(): copy them from the 8.0.0 libs/head.ftl\n", file
    count["ERROR"]++
  }
}
FNR == 1 && NR > 1 { checkHead(previous) }
FNR == 1 { messageJsDefined = 0; messageAttrDefined = 0; previous = FILENAME }
# output formats: fmt[0] is the one of the file (HTML, or set by the <#ftl> header), then one per open <#outputformat>
function isPlain(f) { return f == "plainText" || f == "JavaScript" || f == "JSON" }
FNR == 1 { depth = 0; fmt[0] = "HTML"; plain = 0; script = 0; comment = 0; noparse = 0; xtemplate = 0; messageMacro = 0; vueApp = 0; vuePre = 0 }
{
  line = $0
  # FreeMarker comments and noparse blocks are not output
  if (comment) { if (line ~ /-->/) { comment = 0; sub(/.*-->/, "", line) } else next }
  gsub(/<#--.*-->/, "", line)
  if (line ~ /<#--/) { comment = 1; sub(/<#--.*/, "", line) }
  gsub(/<#noparse>.*<\/#noparse>/, "", line)
  if (noparse) { if (line ~ /<\/#noparse>/) noparse = 0; next }
  if (line ~ /<#noparse>/ && line !~ /<\/#noparse>/) { noparse = 1; next }

  # a plainText, JavaScript or JSON header applies to the whole file (a script-only include); other formats are reported below
  if (FNR == 1 || depth == 0) if (match(line, /<#ftl[^>]*output_format=("[^"]*"|\047[^\047]*\047)/)) {
    f = substr(line, RSTART, RLENGTH); sub(/.*output_format=./, "", f); f = substr(f, 1, length(f) - 1)
    if (isPlain(f)) fmt[0] = f
  }
  # nested output formats are opened at the start of the line and closed at its end
  s = line
  while (match(s, /<#outputformat[ \t]+("[^"]*"|\047[^\047]*\047)/)) {
    f = substr(s, RSTART, RLENGTH); s = substr(s, RSTART + RLENGTH)
    sub(/^<#outputformat[ \t]+./, "", f); fmt[++depth] = substr(f, 1, length(f) - 1)
  }
  plain = isPlain(fmt[depth])
  # each script tag of the line, any case; a script loaded with src does not hide an inline one after it
  lc = tolower(line); s = lc
  while (match(s, /<script([ \t][^>]*)?(>|$)/)) {
    tag = substr(s, RSTART, RLENGTH); s = substr(s, RSTART + RLENGTH)
    if (tag ~ /[ \t]src[ \t]*=/) continue
    script = 1
    # HTML templates for Vue: their content is HTML
    xtemplate = tag ~ /type="text\/(x-template|template|html)"/
  }
  inScript = script && !xtemplate

  if (line ~ /<#function[ \t]+messageJs[ \t>]/) messageJsDefined = 1
  if (line ~ /<#function[ \t]+messageAttr[ \t>]/) messageAttrDefined = 1

  # template errors
  if (line ~ /<#escape[ \t>]/) report("ERROR", "#escape is not allowed with auto-escaping, remove it with its #noescape")
  if (line ~ /<#ftl[^>]*output_format/ && line !~ /output_format=("|\047)(HTML|plainText|JavaScript|JSON)("|\047)/) report("ERROR", "the <#ftl output_format> header turns auto-escaping off or changes it, remove it (plainText, JavaScript and JSON are fine for a script-only file)")
  if (!plain && match(line, /(\$\{[^}]*|<[#@][^>]*)\?(html|xhtml|xml|web_safe)([^a-zA-Z_]|$)/)) {
    b = substr(line, RSTART, RLENGTH); sub(/.*\?/, "?", b); sub(/[^a-zA-Z_?]$/, "", b)
    report("ERROR", b " is an error with auto-escaping, remove it (it still works in a plainText block)")
  }
  if (plain && line ~ /\?no_esc([^a-zA-Z_]|$)/) report("ERROR", "?no_esc is an error inside a plainText block, remove it")

  # scripts
  if (inScript && !plain && line ~ /\$\{/) report("ERROR", "value in a <script> outside <#outputformat \"plainText\">: JSON breaks, quotes show as &#39;")
  # translations: the message macro prints them as is, a quote in them ends the JavaScript string or the attribute
  # (directives removed, their quotes are not the ones of the HTML; a > in a string or in parentheses does not end them)
  m = line; gsub(/<\/?#("[^"]*"|\([^)]*\)|[^>"(])*>/, "", m)
  if (inScript && m ~ /<@message[ \t]/) report("WARN", "translation in a script: a quote in it breaks the script, use ${messageJs(\"code\")}")
  if (!inScript && m ~ /\{\{[^}]*<@message[ \t]/) report("WARN", "translation in a Vue {{ }} expression: a quote in it breaks the page, use ${messageJs(\"code\")}")
  # each attribute, double or single quoted; the script ones are reported below
  s = inScript ? "" : m
  while (match(s, /[ \t][a-zA-Z@:][a-zA-Z0-9_.:@-]*=("[^"]*|\047[^\047]*)<@message[ \t]/)) {
    a = substr(s, RSTART + 1, RLENGTH - 1); s = substr(s, RSTART + RLENGTH)
    sub(/=.*/, "", a)
    if (a !~ /^(on[a-z]+|:[a-zA-Z.-]+|v-[a-z:.-]+|@[a-zA-Z.:-]+)$/) report("WARN", "translation in an attribute: a quote in it breaks the tag, use ${messageAttr(\"code\")}")
  }
  if (inScript && line ~ /\$\{/) {
    s = line
    while (match(s, /\$\{[^}]*\}/)) {
      v = substr(s, RSTART, RLENGTH); s = substr(s, RSTART + RLENGTH)
      if (v ~ /\?(js_string|c|json_string)\}$/ || v ~ /^\$\{messageJs\(/ || configValue(v) || systemId(v)) continue
      if (v ~ /(localize(Raw)?\(|esc\()/) report("WARN", "localize()/esc() in a script no longer escape quotes, add ?js_string: " v)
      else if (userValue(v)) report("ERROR", "user input in a script is no longer HTML-encoded by Mica, it can inject JavaScript: add ?js_string: " v)
      else report("INFO", "script value without ?js_string (or ?c for a number): " v)
    }
  }
  # JavaScript in HTML attributes: the HTML escaping is undone by the browser before the script runs
  s = line
  while (match(s, /[ \t](on[a-z]+|:[a-zA-Z.-]+|v-[a-z:.-]+|@[a-zA-Z.:-]+)="[^"]*(\$\{|<@message)[^"]*"/)) {
    a = substr(s, RSTART, RLENGTH); s = substr(s, RSTART + RLENGTH)
    if (a ~ /<@message[ \t]/) report("WARN", "translation in JavaScript of an attribute: a quote in it breaks the script, use ${messageJs(\"code\")}")
    t = a; bad = 0
    while (match(t, /\$\{[^}]*\}/)) {
      v = substr(t, RSTART, RLENGTH); t = substr(t, RSTART + RLENGTH)
      if (v !~ /\?(js_string|c)\}$/ && v !~ /^\$\{messageJs\(/ && !configValue(v) && !systemId(v)) bad = 1
    }
    if (bad) report("WARN", "value in JavaScript of an attribute without ?js_string")
  }

  # translations can hold HTML: printed escaped, their tags show
  if (line ~ /<#macro[ \t]+message[ \t>]/) messageMacro = 1
  if (messageMacro && !plain && line ~ /\$\{text\}/) report("WARN", "translation printed escaped in the message macro, its HTML shows as text: print it with ${text?no_esc}")
  # the error page: status and msg can come from the request, Mica 7 sent them HTML-encoded
  errorValue = FILENAME ~ /(^|\/)error\.ftl$/ && line ~ /\$\{([^}]*[^a-zA-Z_.])?(msg|status|code)([^a-zA-Z_]|$)/
  if (errorValue && line ~ /\$\{([^}]*[^a-zA-Z_.])?(msg|status|code)([^a-zA-Z_][^}]*)?\?no_esc/) report("ERROR", "status and msg are no longer HTML-encoded by Mica, ?no_esc on them injects HTML: copy the status and message lines from the 8.0.0 error.ftl")
  else if (errorValue && line ~ /\$\{[^}]*getMessage\(/) report("WARN", "translation printed escaped, its HTML shows as text: copy the status and message lines from the 8.0.0 error.ftl (?no_esc on the message injects HTML)")
  else if (!plain && line ~ /\$\{[^}]*getMessage\(/ && line !~ /getMessage\([^}]*\?(no_esc|js_string)/) report("WARN", "translation printed escaped, its HTML shows as text: add ?no_esc")
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
      # translations are written by the administrators, as with the message macro
      if (v !~ /(\?c|\?size|[cC]ount)\}$/ && v !~ /^\$\{message(Js|Attr)\(/ && !configValue(v) && !systemId(v)) bad = 1
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
  # user input that Mica 7 sent HTML-encoded and Mica 8 sends raw: the template escaping is the only protection
  s = line
  while (match(s, /\$\{[^}]*\}/)) {
    v = substr(s, RSTART, RLENGTH); s = substr(s, RSTART + RLENGTH)
    if (!userValue(v)) continue
    if (v ~ /\?no_esc([^a-zA-Z_]|$)/) report("ERROR", "user input printed with ?no_esc, it is no longer HTML-encoded by Mica: remove ?no_esc")
    else if (v ~ /htmlDecode\(/) report("INFO", "user names and attributes, data access titles and query are no longer HTML-encoded by Mica, htmlDecode() can be removed: " v)
  }
  if (line ~ /(^|[^a-zA-Z_.])esc\(/) report("INFO", "esc() is deprecated and returns the value unchanged, remove it")

  if (lc ~ /<\/script[ \t]*>/) { script = 0; xtemplate = 0 }
  n = occurrences(line, "</#outputformat>"); while (n-- > 0 && depth > 0) depth--
}
END {
  if (NR > 0) checkHead(previous)
  printf "%d error(s), %d warning(s), %d to review\n", count["ERROR"], count["WARN"], count["INFO"]
  exit count["ERROR"] > 0
}' "${files[@]}"
