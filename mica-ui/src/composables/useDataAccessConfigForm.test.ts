import { describe, expect, it } from 'vitest';
import { DataAccessFormDto_PdfDownloadType } from 'src/models/Mica';
import { DATA_ACCESS_FORM_KINDS, dataAccessConfigSource } from './useDataAccessConfigForm';

describe('dataAccessConfigSource', () => {
  it('edits the draft of each form, with revisions', () => {
    expect(DATA_ACCESS_FORM_KINDS.map((kind) => dataAccessConfigSource(kind).path)).toEqual([
      '/config/data-access-form',
      '/config/data-access-preliminary-form',
      '/config/data-access-feasibility-form',
      '/config/data-access-amendment-form',
      '/config/data-access-agreement-form',
    ]);
    const source = dataAccessConfigSource('preliminary');
    expect(source.params).toEqual({ revision: 'draft' });
    expect(source.revisions).toBe(true);
    expect(source.legacyIsDirty).toBe(true);
  });

  it('sends the other fields of the DTO back, not the ones set by the server', () => {
    const source = dataAccessConfigSource('application');
    expect(
      source.payload({
        schema: '{}',
        definition: '{}',
        translations: '{}',
        revision: 0,
        lastUpdateDate: '2026-09-21',
        pdfTemplates: [],
        properties: [],
        titleFieldPath: 'projectTitle',
        pdfDownloadType: DataAccessFormDto_PdfDownloadType.Template,
      }),
    ).toEqual({ pdfTemplates: [], properties: [], titleFieldPath: 'projectTitle', pdfDownloadType: 'Template' });
  });
});
