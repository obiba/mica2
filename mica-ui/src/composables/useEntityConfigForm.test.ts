import { describe, expect, it } from 'vitest';
import { EntityFormDto_Type } from 'src/models/Mica';
import { entityConfigSource } from './useEntityConfigForm';

describe('entityConfigSource', () => {
  it('edits the custom form of the document type, sent back with its type', () => {
    const source = entityConfigSource({ name: 'network', type: EntityFormDto_Type.Network });
    expect(source.path).toBe('/config/network/form-custom');
    expect(source.params).toBeUndefined();
    expect(source.revisions).toBeUndefined();
    expect(source.payload({ schema: '{}', definition: '{}' })).toEqual({ type: 'Network' });
  });

  it('sends the project form without type, as its own DTO', () => {
    expect(entityConfigSource({ name: 'project' }).payload({ schema: '{}', definition: '{}' })).toEqual({});
  });
});
