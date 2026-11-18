// Mica.encodeRqlSafe / decodeRqlSafe from _templates/libs/special-char-codec.ftl, run against the real codec map
import { describe, it, expect } from 'vitest';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const map: Record<string, string> = JSON.parse(
  readFileSync(resolve(__dirname, '../../../../mica-core/src/main/resources/codec/special-char-map.json'), 'utf8'),
);
const ftl = readFileSync(resolve(__dirname, '../resources/_templates/libs/special-char-codec.ftl'), 'utf8');
// the FreeMarker list renders the map entries: substitute them, keep the script body as is
const script = ftl
  .slice(ftl.indexOf('<script>') + '<script>'.length, ftl.lastIndexOf('</script>'))
  .replace(/<#if[\s\S]*<\/#if>/, JSON.stringify(map).slice(1, -1))
  .replace(/<\/?#noparse>/g, '');
const Mica: { encodeRqlSafe: (s: unknown) => unknown; decodeRqlSafe: (s: unknown) => unknown } = {} as never;
new Function('Mica', script)(Mica);

describe('special char codec', () => {
  it('encodes and decodes every mapped character', () => {
    for (const [char, encoded] of Object.entries(map)) {
      expect(Mica.encodeRqlSafe(`a${char}b`)).toBe(`a${encoded}b`);
      expect(Mica.decodeRqlSafe(`a${encoded}b`)).toBe(`a${char}b`);
    }
  });

  it('leaves other characters unchanged (#4745)', () => {
    for (const s of ['x\\y', 'a"b', 'abc', '[]^-.*', 'constructor']) {
      expect(Mica.encodeRqlSafe(s)).toBe(s);
    }
    expect(Mica.encodeRqlSafe('x\\y&z')).toBe('x\\yrqlx26rqlz');
    expect(Mica.encodeRqlSafe(1)).toBe(1);
  });
});
