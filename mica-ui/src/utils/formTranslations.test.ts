import { describe, expect, it } from 'vitest';
import {
  flattenMessages,
  resolveTokens,
  tokenKey,
  tokenKeys,
  toToken,
  translatableStrings,
  unwrapKeys,
  wrapKeys,
} from './formTranslations';

describe('tokenKey', () => {
  it('reads the key of a single token', () => {
    expect(tokenKey('t(website)')).toBe('website');
    expect(tokenKey('t( network.name )')).toBe('network.name');
    expect(toToken('website')).toBe('t(website)');
  });

  it('ignores literals and mixed texts', () => {
    expect(tokenKey('Website')).toBeUndefined();
    expect(tokenKey('<h3>t(network.general-info)</h3>')).toBeUndefined();
    expect(tokenKey(42)).toBeUndefined();
  });
});

describe('unwrapKeys', () => {
  it('replaces the single tokens of the known keys of a schema and a UI schema by their keys', () => {
    const known = new Set(['website', 'website.help', 'kind.a', 'website.hint', 'group', 'uri', 'a', 'x']);
    const isKnown = (key: string) => known.has(key);
    const schema = {
      type: 'object',
      properties: {
        website: { title: 't(website)', description: 't(website.help)', type: 'string', format: 't(uri)' },
        kind: { type: 'string', oneOf: [{ const: 't(a)', title: 't(kind.a)' }] },
      },
    };
    const uischema = {
      type: 'VerticalLayout',
      elements: [
        { type: 'Label', text: '<h3>t(network.general-info)</h3>' },
        { type: 'Control', scope: '#/properties/website', options: { hint: 't(website.hint)' } },
        { type: 'Group', label: 't(group)', rule: { effect: 'SHOW', condition: { expr: 't(x)' } } },
        { type: 'Label', text: 't(legacy.key)' },
      ],
    };
    expect(unwrapKeys(schema, isKnown)).toEqual({
      type: 'object',
      properties: {
        website: { title: 'website', description: 'website.help', type: 'string', format: 't(uri)' },
        kind: { type: 'string', oneOf: [{ const: 't(a)', title: 'kind.a' }] },
      },
    });
    expect(unwrapKeys(uischema, isKnown)).toEqual({
      type: 'VerticalLayout',
      elements: [
        { type: 'Label', text: '<h3>t(network.general-info)</h3>' },
        { type: 'Control', scope: '#/properties/website', options: { hint: 'website.hint' } },
        { type: 'Group', label: 'group', rule: { effect: 'SHOW', condition: { expr: 't(x)' } } },
        // a token of an unknown key (Mica bundle) is a literal
        { type: 'Label', text: 't(legacy.key)' },
      ],
    });
  });
});

describe('wrapKeys', () => {
  it('writes the known keys as tokens, but for the untranslated ones', () => {
    const isKnown = (key: string) => ['name.title', 'name.hint', 'name'].includes(key);
    expect(
      wrapKeys(
        {
          type: 'object',
          properties: { name: { type: 'string', title: 'name.title', description: 'Literal', default: 'name' } },
          required: ['name'],
        },
        isKnown,
      ),
    ).toEqual({
      type: 'object',
      properties: { name: { type: 'string', title: 't(name.title)', description: 'Literal', default: 'name' } },
      required: ['name'],
    });
    expect(
      wrapKeys(
        {
          type: 'Control',
          scope: '#/properties/name',
          hint: 'name.hint',
          options: { labels: ['name.title', 'Other'] },
        },
        isKnown,
      ),
    ).toEqual({
      type: 'Control',
      scope: '#/properties/name',
      hint: 't(name.hint)',
      options: { labels: ['t(name.title)', 'Other'] },
    });
  });

  it('is the reverse of unwrapKeys', () => {
    const isKnown = (key: string) => key.startsWith('name.');
    const form = { properties: { name: { title: 'name.title', enum: ['name.title'] } } };
    expect(unwrapKeys(wrapKeys(form, isKnown), isKnown)).toEqual(form);
  });
});

describe('tokens', () => {
  it('lists the keys of the tokens of a text', () => {
    expect(tokenKeys('<h3>t(network.info)</h3><p>t( website )</p>')).toEqual(['network.info', 'website']);
    expect(tokenKeys('Website')).toEqual([]);
    expect(tokenKeys(42)).toEqual([]);
  });

  it('resolves the tokens of a text from the messages, keeping the unknown ones', () => {
    expect(resolveTokens('<h3>t(network.info)</h3> t(nope)', { 'network.info': 'Information' })).toBe(
      '<h3>Information</h3> t(nope)',
    );
  });

  it('flattens a bundle to dotted keys', () => {
    expect(flattenMessages({ a: { b: 'B', c: { d: 'D' } }, e: 'E', f: 1 })).toEqual({
      'a.b': 'B',
      'a.c.d': 'D',
      e: 'E',
    });
    expect(flattenMessages('nope')).toEqual({});
  });

  it('lists the strings that may hold tokens', () => {
    expect(
      translatableStrings({
        type: 'object',
        properties: { x: { title: 't(x)', type: 'string', enum: ['a'] } },
        required: ['x'],
      }),
    ).toEqual(['t(x)']);
  });
});
