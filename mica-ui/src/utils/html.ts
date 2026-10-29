/**
 * The text of a value stored HTML-encoded by the server (comment messages), plain text is returned unchanged.
 * The result is plain text: display it as text or as markdown without raw HTML, never with v-html.
 */
export function decodeHtml(text: string | undefined): string {
  if (!text) return '';
  // a textarea content is never parsed as markup: no element is created and no script can run
  const textarea = document.createElement('textarea');
  textarea.innerHTML = text;
  return textarea.value;
}
