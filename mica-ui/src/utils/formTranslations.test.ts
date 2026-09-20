import { describe, expect, it } from 'vitest';
import { flattenMessages, tokenKey, toToken, unwrapKeys } from './formTranslations';

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

describe('messages', () => {
  it('flattens a nested bundle to dotted keys', () => {
    expect(flattenMessages({ website: 'Website', network: { name: 'Name', general: { info: 'Info' } }, n: 1 })).toEqual({
      website: 'Website',
      'network.name': 'Name',
      'network.general.info': 'Info',
    });
    expect(flattenMessages(undefined)).toEqual({});
  });
});
