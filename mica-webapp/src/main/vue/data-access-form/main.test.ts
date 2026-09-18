import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { nextTick } from 'vue';
import type { MountOptions } from './types';

// page globals of scripts.ftl / data-access-scripts.ftl
const MicaService = {
  normalizeUrl: vi.fn((url: string) => `/mica${url}`),
  toastSuccess: vi.fn(),
  toastWarning: vi.fn(),
  toastError: vi.fn(),
  redirect: vi.fn(),
};
const DataAccessService = { submit: vi.fn(), approve: vi.fn() };
const axios = { put: vi.fn() };
vi.stubGlobal('MicaService', MicaService);
vi.stubGlobal('DataAccessService', DataAccessService);
vi.stubGlobal('axios', axios);
vi.stubGlobal('fetch', vi.fn(async () => ({ ok: true, json: async () => ({}) })));

const messages = { validationSuccess: 'valid', validationError: 'invalid', validationErrorOnSubmit: 'cannot submit', errorOnSave: 'save failed' };

// an angular-schema-form pair, as the server injects it: a required name, a conditional required field
const schema = {
  type: 'object',
  properties: {
    name: { type: 'string', title: 'Name' },
    other: { type: 'boolean', title: 'Other?' },
    otherText: { type: 'string', title: 'Which?' },
  },
  required: ['name'],
};
const definition = ['name', 'other', { key: 'otherText', required: true, condition: 'model.other' }];

const api = () => window.MicaDataAccessForm;
const flush = async () => { for (let i = 0; i < 5; i++) await nextTick(); };

function mountForm(model: Record<string, any>, readOnly = false) {
  document.body.innerHTML = '<div id="data-access-form"></div>';
  const options: MountOptions = { schema, definition, model, readOnly, lang: 'en', contextPath: '/mica', messages };
  api().mount('#data-access-form', options);
}

describe('MicaDataAccessForm', () => {
  beforeEach(async () => {
    await import('./main');
    vi.clearAllMocks();
  });
  afterEach(() => { document.body.innerHTML = ''; });

  it('mounts the converted form in the page element', async () => {
    mountForm({ name: 'Jane' });
    await flush();
    const root = document.querySelector('#data-access-form')!;
    expect(root.classList.contains('mica-json-form')).toBe(true);
    expect(root.querySelectorAll('.q-field').length).toBe(1); // otherText hidden by its condition
    expect((root.querySelector('input') as HTMLInputElement).value).toBe('Jane');
    expect(root.querySelector('.q-toggle')).not.toBeNull();
  });

  it('logs an error when the mount element is missing', () => {
    const error = vi.spyOn(console, 'error').mockImplementation(() => {});
    document.body.innerHTML = '';
    api().mount('#nope', { schema, definition, model: {}, readOnly: false, lang: 'en', contextPath: '', messages });
    expect(error).toHaveBeenCalledWith(expect.stringContaining("'#nope' not found"));
    error.mockRestore();
  });

  it('validates with a toast, showing the errors only from then on', async () => {
    mountForm({});
    await flush();
    expect(document.querySelectorAll('.q-field--error').length).toBe(0);
    expect(api().validate()).toBe(false);
    await flush();
    expect(MicaService.toastWarning).toHaveBeenCalledWith('invalid');
    expect(document.querySelectorAll('.q-field--error').length).toBe(1);
    expect(api().errors().map((e) => e.keyword)).toEqual(['required']);
  });

  it('ignores the required fields hidden by a condition', async () => {
    mountForm({ name: 'Jane', other: false });
    await flush();
    expect(api().validate()).toBe(true);
    expect(MicaService.toastSuccess).toHaveBeenCalledWith('valid');
    mountForm({ name: 'Jane', other: true });
    await flush();
    expect(api().validate()).toBe(false);
    expect(api().errors().map((e) => e.params.missingProperty)).toEqual(['otherText']);
  });

  it('saves the model and redirects to the form page, toasts on failure', async () => {
    mountForm({ name: 'Jane' });
    await flush();
    axios.put.mockResolvedValueOnce({});
    api().save('DAR-1');
    await flush();
    expect(axios.put).toHaveBeenCalledWith('/mica/ws/data-access-request/DAR-1/model', { name: 'Jane' }, { headers: { 'Content-Type': 'application/json' } });
    expect(MicaService.redirect).toHaveBeenCalledWith('/mica/data-access-form/DAR-1');

    axios.put.mockResolvedValueOnce({});
    api().save('DAR-1', 'amendment', 'DAR-1-A1');
    await flush();
    expect(axios.put).toHaveBeenLastCalledWith('/mica/ws/data-access-request/DAR-1/amendment/DAR-1-A1/model', expect.anything(), expect.anything());
    expect(MicaService.redirect).toHaveBeenLastCalledWith('/mica/data-access-amendment-form/DAR-1-A1');

    vi.spyOn(console, 'dir').mockImplementation(() => {});
    axios.put.mockRejectedValueOnce(new Error('boom'));
    api().save('DAR-1');
    await flush();
    expect(MicaService.toastError).toHaveBeenCalledWith('save failed');
  });

  it('submits and approves only a valid form', async () => {
    mountForm({});
    await flush();
    api().submit('DAR-1');
    api().approveAgreement('DAR-1', 'DAR-1-administrator');
    expect(DataAccessService.submit).not.toHaveBeenCalled();
    expect(DataAccessService.approve).not.toHaveBeenCalled();
    expect(MicaService.toastError).toHaveBeenCalledTimes(2);
    expect(MicaService.toastError).toHaveBeenCalledWith('cannot submit');

    mountForm({ name: 'Jane' });
    await flush();
    api().submit('DAR-1', 'feasibility', 'DAR-1-F1');
    api().approveAgreement('DAR-1', 'DAR-1-administrator');
    expect(DataAccessService.submit).toHaveBeenCalledWith('DAR-1', 'feasibility', 'DAR-1-F1');
    expect(DataAccessService.approve).toHaveBeenCalledWith('DAR-1', 'agreement', 'DAR-1-administrator');
  });

  it('renders read-only', async () => {
    mountForm({ name: 'Jane' }, true);
    await flush();
    expect(document.querySelectorAll('.q-field--readonly').length).toBe(1);
  });
});
