import type { FieldItem } from 'src/components/FieldsList.vue';
import type { MicaConfigDto } from 'src/models/Mica';

type ConfigField = keyof MicaConfigDto;

/** the message keys of a configuration field: `config.<key>` (label) and `config.<key>_help` (hint) */
function labels(key: string, withHelp: boolean) {
  return withHelp ? { label: `config.${key}`, hint: `config.${key}_help` } : { label: `config.${key}` };
}

/** a boolean field, shown as a check or a cross */
export function flagItem(field: ConfigField, key: string, withHelp = true): FieldItem {
  return {
    field,
    ...labels(key, withHelp),
    icon: (config: MicaConfigDto) => (config[field] ? 'check' : 'close'),
  };
}

/** a text or number field, shown as is */
export function valueItem(field: ConfigField, key: string, withHelp = true): FieldItem {
  return { field, ...labels(key, withHelp) };
}

/** a field shown through a formatter (lists, labelled options) */
export function formattedItem(
  field: ConfigField,
  key: string,
  format: (config: MicaConfigDto) => string,
  withHelp = true,
): FieldItem {
  return { field, ...labels(key, withHelp), format };
}
