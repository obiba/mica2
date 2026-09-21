import type { EntityFormDto, EntityFormDto_Type } from 'src/models/Mica';
import { useConfigForm, type ConfigFormSource } from 'src/composables/useConfigForm';

/** the configuration resource (`network` for `/config/network/form-custom`) and the type of its form DTO */
export type EntityConfigTarget =
  | { name: string; type: EntityFormDto_Type }
  /** the project form has its own DTO, without type */
  | { name: 'project'; type?: undefined };

/** the form as sent to `/config/{type}/form-custom`: an EntityFormDto, or a ProjectFormDto without type */
export type EntityFormPayload = Omit<EntityFormDto, 'type'> & Partial<Pick<EntityFormDto, 'type'>>;

/** the custom part of the form of a document type, `/config/{type}/form-custom` */
export function entityConfigSource(target: EntityConfigTarget): ConfigFormSource<EntityFormPayload> {
  return {
    path: `/config/${target.name}/form-custom`,
    payload: () => (target.type ? { type: target.type } : {}),
  };
}

/**
 * The custom part of the form configuration of a document type (`/config/{type}/form-custom`),
 * as a form definition for `QJsonFormBuilder`.
 */
export function useEntityConfigForm(target: EntityConfigTarget) {
  return useConfigForm<EntityFormPayload>(entityConfigSource(target));
}
