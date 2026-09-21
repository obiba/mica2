import { defineBoot } from '#q-app';
import { COUNTRIES_KEY, countryCodes } from '@obiba/quasar-ui-json-form';

// the `format: "countries"` renderer of quasar-json-form reads its `{ code, name }` list from the
// `jsonforms-countries` provide (localized map, the current locale is used): provided once here so
// that every form (entity forms, form builder previews) gets it without per-component wiring
export default defineBoot(({ app }) => {
  app.provide(COUNTRIES_KEY, countryCodes);
});
