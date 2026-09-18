import { describe, expect, it } from 'vitest';
import { ref } from 'vue';
import { documentTarget, useDocumentTarget } from './useDocumentTarget';

describe('documentTarget', () => {
  it('derives the REST, files and route paths of a network', () => {
    expect(documentTarget('network', 'abc')).toEqual({
      type: 'network',
      id: 'abc',
      path: '/draft/network/abc',
      collectionPath: '/draft/networks',
      filesPath: '/network/abc',
      formPath: '/config/network/form',
      routeBase: '/network',
    });
  });

  it('knows the collection of every document type', () => {
    expect(documentTarget('individual-study', 'x').collectionPath).toBe('/draft/individual-studies');
    expect(documentTarget('harmonization-study', 'x').collectionPath).toBe('/draft/harmonization-studies');
    expect(documentTarget('collected-dataset', 'x').collectionPath).toBe('/draft/collected-datasets');
    expect(documentTarget('harmonized-dataset', 'x').collectionPath).toBe('/draft/harmonized-datasets');
    expect(documentTarget('project', 'x').collectionPath).toBe('/draft/projects');
  });

  it('follows the id', () => {
    const id = ref('one');
    const { target } = useDocumentTarget('network', id);
    expect(target.value.path).toBe('/draft/network/one');
    id.value = 'two';
    expect(target.value.path).toBe('/draft/network/two');
  });
});
