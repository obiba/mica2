/**
 * The text of a value stored HTML-encoded by the server (comment messages), plain text is returned unchanged.
 * The result is plain text: display it as text or as markdown without raw HTML, never with v-html.
 */
// a textarea content is never parsed as markup: no element is created and no script can run;
// one detached textarea is reused for all the calls, created on first use
let textarea: HTMLTextAreaElement | undefined;

export function decodeHtml(text: string | undefined): string {
  if (!text) return '';
  textarea ??= document.createElement('textarea');
  textarea.innerHTML = text;
  return textarea.value;
}
