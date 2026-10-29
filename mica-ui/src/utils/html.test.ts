import { describe, expect, it } from 'vitest';
import { decodeHtml } from './html';

describe('html', () => {
  it('decodes a comment message stored encoded', () => {
    // as stored by Comment.Builder (Spring HtmlUtils.htmlEscape)
    expect(decodeHtml('&lt;b&gt;&amp;&quot;&#39;&eacute;&rsquo; x@y =1+1 &gt; quote')).toBe(`<b>&"'é’ x@y =1+1 > quote`);
    expect(decodeHtml('line 1\nline 2')).toBe('line 1\nline 2');
  });

  it('keeps plain text unchanged', () => {
    ['Tom & Jerry', 'R&D, 50% & more', 'a < b > c', 'plain'].forEach((text) => expect(decodeHtml(text)).toBe(text));
  });

  it('keeps markup as text', () => {
    expect(decodeHtml('&lt;img src=x onerror=alert(1)&gt;')).toBe('<img src=x onerror=alert(1)>');
    expect(decodeHtml('<img src=x onerror=alert(1)>')).toBe('<img src=x onerror=alert(1)>');
    expect(decodeHtml('</textarea><script>alert(1)</script>')).toBe('</textarea><script>alert(1)</script>');
  });

  it('decodes once', () => {
    expect(decodeHtml('&amp;amp;')).toBe('&amp;');
  });

  it('handles empty values', () => {
    expect(decodeHtml(undefined)).toBe('');
    expect(decodeHtml('')).toBe('');
  });
});
