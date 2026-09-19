import { describe, expect, it } from 'vitest';
import { ref } from 'vue';
import { documentTarget, isDocumentType, useDocumentTarget } from './useDocumentTarget';

describe('documentTarget', () => {
  it('derives the REST, files and route paths of a network', () => {
    expect(documentTarget('network', 'abc')).toEqual({
      type: 'network',
      id: 'abc',
      path: '/draft/network/abc',
      collectionPath: '/draft/networks',
      listPath: '/draft/networks',
      listParams: {},
      filesPath: '/network/abc',
      formPath: '/config/network/form',
      routeBase: '/network',
      listRoute: '/networks',
      withLogo: true,
      labels: { title: 'networks.title', new: 'networks.new' },
    });
  });

  it('knows the collection of every document type', () => {
    expect(documentTarget('individual-study', 'x').collectionPath).toBe('/draft/individual-studies');
    expect(documentTarget('harmonization-study', 'x').collectionPath).toBe('/draft/harmonization-studies');
    expect(documentTarget('collected-dataset', 'x').collectionPath).toBe('/draft/collected-datasets');
    expect(documentTarget('harmonized-dataset', 'x').collectionPath).toBe('/draft/harmonized-datasets');
    expect(documentTarget('project', 'x').collectionPath).toBe('/draft/projects');
  });

  it('wraps the projects list only', () => {
    const project = documentTarget('project', 'p');
    expect(project.listPath).toBe('/draft/projects');
    expect(project.listKey).toBe('projects');
    expect(project.routeBase).toBe('/project');
    expect(project.listRoute).toBe('/projects');
    expect(project.withLogo).toBe(false);
    expect(documentTarget('network', 'n').listKey).toBeUndefined();
  });

  it('serves the states of the studies apart', () => {
    const study = documentTarget('harmonization-study', 'hs');
    expect(study.statePath).toBe('/draft/study-state/hs');
    expect(study.listPath).toBe('/draft/study-states');
    expect(study.listParams).toEqual({ type: 'harmonization-study' });
    expect(study.withLogo).toBe(true);
    const dataset = documentTarget('harmonized-dataset', 'hd');
    expect(dataset.statePath).toBeUndefined();
    expect(dataset.listPath).toBe('/draft/harmonized-datasets');
    expect(dataset.withLogo).toBe(false);
  });

  it('checks a document type', () => {
    expect(isDocumentType('collected-dataset')).toBe(true);
    expect(isDocumentType('dataset')).toBe(false);
    expect(isDocumentType(undefined)).toBe(false);
  });

  it('follows the type and the id', () => {
    const type = ref<'network' | 'individual-study'>('network');
    const id = ref('one');
    const { target } = useDocumentTarget(type, id);
    expect(target.value.path).toBe('/draft/network/one');
    id.value = 'two';
    expect(target.value.path).toBe('/draft/network/two');
    type.value = 'individual-study';
    expect(target.value.path).toBe('/draft/individual-study/two');
  });
});
