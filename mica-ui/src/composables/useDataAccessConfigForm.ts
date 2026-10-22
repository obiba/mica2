import type {
  DataAccessAgreementFormDto,
  DataAccessAmendmentFormDto,
  DataAccessFeasibilityFormDto,
  DataAccessFormDto,
  DataAccessPreliminaryFormDto,
} from 'src/models/Mica';
import { useConfigForm, type ConfigFormSource } from 'src/composables/useConfigForm';

/** the five data access forms */
export type DataAccessFormKind = 'application' | 'preliminary' | 'feasibility' | 'amendment' | 'agreement';

export const DATA_ACCESS_FORM_KINDS: DataAccessFormKind[] = [
  'application',
  'preliminary',
  'feasibility',
  'amendment',
  'agreement',
];

/** the DTO of each form */
export interface DataAccessFormDtos {
  application: DataAccessFormDto;
  preliminary: DataAccessPreliminaryFormDto;
  feasibility: DataAccessFeasibilityFormDto;
  amendment: DataAccessAmendmentFormDto;
  agreement: DataAccessAgreementFormDto;
}

/** the configuration resource of each form */
export const DATA_ACCESS_FORM_PATHS: Record<DataAccessFormKind, string> = {
  application: '/config/data-access-form',
  preliminary: '/config/data-access-preliminary-form',
  feasibility: '/config/data-access-feasibility-form',
  amendment: '/config/data-access-amendment-form',
  agreement: '/config/data-access-agreement-form',
};

/** the fields set by the server on load, not sent back */
const SERVER_FIELDS = new Set(['schema', 'definition', 'translations', 'revision', 'lastUpdateDate']);

/**
 * A data access form: the draft revision is edited, the other fields of the DTO (field paths, PDF
 * templates...) are sent back as loaded, or as edited by the page through `dto`.
 */
export function dataAccessConfigSource<K extends DataAccessFormKind>(kind: K): ConfigFormSource<DataAccessFormDtos[K]> {
  return {
    path: DATA_ACCESS_FORM_PATHS[kind],
    params: { revision: 'draft' },
    payload: (dto) =>
      Object.fromEntries(Object.entries(dto).filter(([name]) => !SERVER_FIELDS.has(name))) as Record<string, unknown>,
    revisions: true,
    // a form still in the legacy dialect is to be saved and published from here
    legacyIsDirty: true,
  };
}

/** the draft of a data access form (`/config/data-access-*-form?revision=draft`) as a form definition for the builder */
export function useDataAccessConfigForm<K extends DataAccessFormKind>(kind: K) {
  return useConfigForm<DataAccessFormDtos[K]>(dataAccessConfigSource(kind));
}
