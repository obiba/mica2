import type { FieldItem } from 'src/components/FieldsList.vue';
import type { MicaConfigDto } from 'src/models/Mica';

/** the configuration fields of a given value type */
type FieldsOfType<T> = {
  [K in keyof MicaConfigDto]-?: MicaConfigDto[K] extends T | undefined ? K : never;
}[keyof MicaConfigDto];

/** a kind of email notification: an enable flag and an optional subject in the configuration */
export interface NotificationKind {
  /** the message key under `config.notifications`: the label, `<key>_help`, `<key>_subject`, `<key>_subject_help` */
  key: string;
  enabledField: FieldsOfType<boolean>;
  subjectField: FieldsOfType<string>;
  /** the reserved terms the subject can contain */
  terms: string[];
  /** the default subject, as the server builds it: `[org] id: <Type name> status has changed` */
  placeholder: string;
}

const DOCUMENT_TERMS = ['${organization}', '${documentType}', '${documentId}', '${status}'];

/** the status change notifications of the documents */
export const DOCUMENT_KINDS: NotificationKind[] = [
  {
    key: 'network',
    enabledField: 'isNetworkNotificationsEnabled',
    subjectField: 'networkNotificationsSubject',
    terms: DOCUMENT_TERMS,
    placeholder: '[${organization}] ${documentId}: Network status has changed',
  },
  {
    key: 'study',
    enabledField: 'isStudyNotificationsEnabled',
    subjectField: 'studyNotificationsSubject',
    terms: DOCUMENT_TERMS,
    // the type name is the one of the document: an individual study or a harmonization study
    placeholder: '[${organization}] ${documentId}: Individual Study / Harmonization Study status has changed',
  },
  {
    key: 'collected_dataset',
    enabledField: 'isCollectedDatasetNotificationsEnabled',
    subjectField: 'collectedDatasetNotificationsSubject',
    terms: DOCUMENT_TERMS,
    placeholder: '[${organization}] ${documentId}: Collected Dataset status has changed',
  },
  {
    key: 'harmonized_dataset',
    enabledField: 'isHarmonizedDatasetNotificationsEnabled',
    subjectField: 'harmonizedDatasetNotificationsSubject',
    terms: DOCUMENT_TERMS,
    placeholder: '[${organization}] ${documentId}: Harmonized Dataset status has changed',
  },
  {
    key: 'project',
    enabledField: 'isProjectNotificationsEnabled',
    subjectField: 'projectNotificationsSubject',
    terms: DOCUMENT_TERMS,
    placeholder: '[${organization}] ${documentId}: Project status has changed',
  },
];

/** the file status change and the comment notifications */
export const FILE_COMMENT_KINDS: NotificationKind[] = [
  {
    key: 'file',
    enabledField: 'isFsNotificationsEnabled',
    subjectField: 'fsNotificationsSubject',
    terms: [...DOCUMENT_TERMS, '${path}'],
    placeholder: '[${organization}] ${documentId}: file status has changed',
  },
  {
    key: 'comment',
    enabledField: 'isCommentNotificationsEnabled',
    subjectField: 'commentNotificationsSubject',
    terms: DOCUMENT_TERMS,
    placeholder: '[${organization}] ${documentId} was commented',
  },
];

/** the contact request notification, sent to the members of the contact groups */
export const CONTACT_KIND: NotificationKind = {
  key: 'contact',
  enabledField: 'isContactNotificationsEnabled',
  subjectField: 'contactNotificationsSubject',
  terms: ['${organization}', '${contactName}', '${contactSubject}'],
  placeholder: '[${organization}] Contact',
};

function escapeHtml(text: string): string {
  return text.replace(/[&<>"']/g, (char) => `&#${char.charCodeAt(0)};`);
}

/**
 * The view row of a kind: a check or a cross, then the subject when set, or the default one
 * (greyed) when the notification is enabled without a subject.
 */
export function notificationItem(kind: NotificationKind): FieldItem {
  return {
    field: kind.enabledField,
    label: `config.notifications.${kind.key}`,
    hint: `config.notifications.${kind.key}_help`,
    html: (config: MicaConfigDto) => {
      const enabled = config[kind.enabledField] === true;
      const subject = config[kind.subjectField];
      const icon = `<i class="q-icon material-icons">${enabled ? 'check' : 'close'}</i>`;
      if (subject) return `${icon} <span class="q-ml-xs">${escapeHtml(subject)}</span>`;
      if (enabled)
        return `${icon} <span class="q-ml-xs text-grey-6 text-italic">${escapeHtml(kind.placeholder)}</span>`;
      return icon;
    },
  };
}
