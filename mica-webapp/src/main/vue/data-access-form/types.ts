import type { ErrorObject } from 'ajv';

/** Toast messages, translated by the Freemarker template (`formMessages` global). */
export interface FormMessages {
  validationSuccess: string;
  validationError: string;
  validationErrorOnSubmit: string;
  errorOnSave: string;
}

/** Options of `MicaDataAccessForm.mount()`, built by data-access-form-scripts.ftl. */
export interface MountOptions {
  /** JSON schema (`t()` tokens already resolved by the server) */
  schema: Record<string, any>;
  /**
   * JSON Forms UI schema (object), or the angular-schema-form definition (array) of a form revision
   * that a request was submitted with before the forms were edited as JSON Forms: read-only rendering,
   * converted on the fly
   */
  definition: unknown;
  /** form data */
  model: Record<string, any>;
  readOnly: boolean;
  /** page language (`${.lang}`) */
  lang: string;
  /** Mica context path (`contextPath` global) */
  contextPath: string;
  messages: FormMessages;
}

/** Page globals defined by scripts.ftl / data-access-scripts.ftl. */
declare global {
  interface Window {
    MicaDataAccessForm: MicaDataAccessFormApi;
  }
  const MicaService: {
    normalizeUrl(url: string): string;
    toastSuccess(text: string): void;
    toastWarning(text: string): void;
    toastError(text: string): void;
    redirect(path: string): void;
  };
  const DataAccessService: {
    submit(id: string, type?: string, aId?: string): void;
    approve(id: string, type?: string, aId?: string): void;
  };
  const axios: {
    put(url: string, data: any, config?: any): Promise<any>;
  };
}

export interface MicaDataAccessFormApi {
  mount(selector: string, options: MountOptions): void;
  /** validates the form and toasts the result (an invalid form can still be saved) */
  validate(): boolean;
  /** saves the model and redirects to the form page */
  save(id: string, type?: string, aId?: string): void;
  /** validates, then submits the request / preliminary / feasibility / amendment */
  submit(id: string, type?: string, aId?: string): void;
  /** validates, then approves the agreement */
  approveAgreement(id: string, aId: string): void;
  /** current validation errors (AJV-shaped), for support / debugging from the browser console */
  errors(): ErrorObject[];
}
