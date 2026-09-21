import type { DocumentType } from 'src/composables/useDocumentTarget';
import type { EntityConfigTarget } from 'src/composables/useEntityConfigForm';
import { EntityFormDto_Type, type MicaConfigDto } from 'src/models/Mica';

/** a form edited on the settings page of a document type */
export interface EntityConfigForm {
  target: EntityConfigTarget;
  /** i18n key of the form title */
  label: string;
  /** i18n keys of the parameters of the form info (`config.form_info`): the document type name and its mandatory fields */
  info: { type: string; fields: string };
}

/** the settings page of a document type: its forms and the permissions on its documents */
export interface EntityConfig {
  /** i18n key of the settings entry */
  label: string;
  /** i18n key of the settings entry caption */
  caption: string;
  forms: EntityConfigForm[];
  /** the permissions on any draft document and the accesses to any published one: `/config/{name}/permissions` */
  permissions: string;
  /** whether the section is enabled, by the Mica configuration */
  isEnabled: (configuration: MicaConfigDto) => boolean;
}

function form(target: EntityConfigTarget, key: string): EntityConfigForm {
  return {
    target,
    label: `config.${key}_form`,
    info: { type: `config.${key}_form_type`, fields: `config.${key}_form_fields` },
  };
}

const ENTITY_CONFIGS: Record<DocumentType, EntityConfig> = {
  network: {
    label: 'settings.network',
    caption: 'settings.network_caption',
    forms: [form({ name: 'network', type: EntityFormDto_Type.Network }, 'network')],
    permissions: 'network',
    isEnabled: (configuration) => configuration.isNetworkEnabled === true,
  },
  'individual-study': {
    label: 'settings.individual_study',
    caption: 'settings.individual_study_caption',
    forms: [
      form({ name: 'individual-study', type: EntityFormDto_Type.Study }, 'individual_study'),
      form({ name: 'population', type: EntityFormDto_Type.Population }, 'population'),
      form({ name: 'data-collection-event', type: EntityFormDto_Type.DataCollectionEvent }, 'data_collection_event'),
    ],
    permissions: 'individual-study',
    isEnabled: () => true,
  },
  'harmonization-study': {
    label: 'settings.harmonization_study',
    caption: 'settings.harmonization_study_caption',
    forms: [form({ name: 'harmonization-study', type: EntityFormDto_Type.HarmonizationStudy }, 'harmonization_study')],
    permissions: 'harmonization-study',
    isEnabled: () => true,
  },
  'collected-dataset': {
    label: 'settings.collected_dataset',
    caption: 'settings.collected_dataset_caption',
    forms: [form({ name: 'collected-dataset', type: EntityFormDto_Type.CollectedDataset }, 'collected_dataset')],
    permissions: 'collected-dataset',
    isEnabled: (configuration) => configuration.isCollectedDatasetEnabled === true,
  },
  'harmonized-dataset': {
    label: 'settings.harmonized_dataset',
    caption: 'settings.harmonized_dataset_caption',
    forms: [form({ name: 'harmonized-dataset', type: EntityFormDto_Type.HarmonizedDataset }, 'harmonized_dataset')],
    permissions: 'harmonized-dataset',
    isEnabled: (configuration) => configuration.isHarmonizedDatasetEnabled === true,
  },
  project: {
    label: 'settings.project',
    caption: 'settings.project_caption',
    forms: [form({ name: 'project' }, 'project')],
    permissions: 'project',
    isEnabled: (configuration) => configuration.isProjectEnabled === true,
  },
};

export function entityConfig(type: DocumentType): EntityConfig {
  return ENTITY_CONFIGS[type];
}

/** the settings route of the document type */
export function entityConfigRoute(type: DocumentType): string {
  return `/settings/${type}`;
}
