import { createApp } from 'vue';
import type { ComponentPublicInstance } from 'vue';
import type { ErrorObject } from 'ajv';
import { Quasar } from 'quasar';
import langEn from 'quasar/lang/en-US';
import langFr from 'quasar/lang/fr';
import JsonFormPlugin from '@obiba/quasar-ui-json-form';
import 'quasar/dist/quasar.css';
import '@quasar/extras/material-icons/material-icons.css';
import '@obiba/quasar-ui-json-form/dist/index.css';
import './styles.css';
import DataAccessForm from './DataAccessForm.vue';
import { createFormI18n, loadMicaTranslations } from './i18n';
import { createFileUploadHooks } from './files';
import type { FormMessages, MicaDataAccessFormApi, MountOptions } from './types';

/** class of the form root and of the Quasar portal nodes (menus, dialogs), see vite.forms.config.mjs */
const ROOT_CLASS = 'mica-json-form';

const QUASAR_LANGS: Record<string, any> = { en: langEn, fr: langFr };

type FormInstance = ComponentPublicInstance & { validate(): boolean; getModel(): Record<string, any>; getErrors(): ErrorObject[] };

let form: FormInstance | null = null;
let messages: FormMessages | null = null;
let contextPath = '';

function mount(selector: string, options: MountOptions): void {
  const el = document.querySelector(selector);
  if (!el) {
    console.error(`[data-access-form] mount element '${selector}' not found: the page template must contain it`);
    return;
  }
  el.classList.add(ROOT_CLASS);
  contextPath = options.contextPath || '';
  messages = options.messages;

  const i18n = createFormI18n(options.lang);
  const app = createApp(DataAccessForm, {
    schema: options.schema,
    definition: options.definition,
    modelValue: options.model,
    readOnly: options.readOnly,
    lang: options.lang,
    fileUpload: createFileUploadHooks(contextPath),
  });
  app.use(Quasar, {
    lang: QUASAR_LANGS[options.lang] || langEn,
    config: { globalNodes: { class: ROOT_CLASS } },
  });
  app.use(i18n);
  app.use(JsonFormPlugin);
  form = app.mount(el) as FormInstance;

  loadMicaTranslations(i18n, contextPath, options.lang).catch((error) => console.warn('[data-access-form]', error));
}

/** current validation errors (AJV-shaped), for support / debugging from the browser console */
function errors(): ErrorObject[] {
  return ensureMounted().getErrors();
}

function ensureMounted(): FormInstance {
  if (!form || !messages) {
    throw new Error('[data-access-form] mount() must be called first');
  }
  return form;
}

function validate(): boolean {
  const valid = ensureMounted().validate();
  if (valid) {
    MicaService.toastSuccess(messages!.validationSuccess);
  } else {
    // an invalid form can be saved with warning
    MicaService.toastWarning(messages!.validationError);
  }
  return valid;
}

function save(id: string, type?: string, aId?: string): void {
  const instance = ensureMounted();
  let url = `/ws/data-access-request/${id}/model`;
  let redirect = `/data-access-form/${id}`;
  if (type && aId) {
    url = `/ws/data-access-request/${id}/${type}/${aId}/model`;
    redirect = `/data-access-${type}-form/${aId}`;
  }
  axios
    .put(MicaService.normalizeUrl(url), instance.getModel(), { headers: { 'Content-Type': 'application/json' } })
    .then(() => MicaService.redirect(MicaService.normalizeUrl(redirect)))
    .catch((response: unknown) => {
      MicaService.toastError(messages!.errorOnSave);
      console.dir(response);
    });
}

function submit(id: string, type?: string, aId?: string): void {
  if (ensureMounted().validate()) {
    DataAccessService.submit(id, type, aId);
  } else {
    // an invalid form cannot be submitted
    MicaService.toastError(messages!.validationErrorOnSubmit);
  }
}

function approveAgreement(id: string, aId: string): void {
  if (ensureMounted().validate()) {
    DataAccessService.approve(id, 'agreement', aId);
  } else {
    MicaService.toastError(messages!.validationErrorOnSubmit);
  }
}

const api: MicaDataAccessFormApi = { mount, validate, save, submit, approveAgreement, errors };
window.MicaDataAccessForm = api;

export default api;
