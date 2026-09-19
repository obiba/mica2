import { api, toServerUrl } from 'src/boot/api';
import type { KeyForm } from 'src/models/Mica';

/** the key pair (or certificate) of the HTTPS connection, in the system key store */
export const SYSTEM_KEY_PATH = '/config/keystore/system/https';

export async function saveSystemKey(keyForm: KeyForm): Promise<void> {
  await api.put(SYSTEM_KEY_PATH, keyForm);
}

/** the PEM certificate of the system key, served as an attachment */
export function systemCertificateUrl(): string {
  return toServerUrl(SYSTEM_KEY_PATH);
}
