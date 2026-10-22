import { api, toServerUrl } from 'src/boot/api';
import type { OpalCredentialDto } from 'src/models/Mica';
import { notifyError } from 'src/utils/notify';

/** the PEM certificate of a key pair credential, served as an attachment */
export function opalCredentialCertificateUrl(opalUrl: string): string {
  return toServerUrl(`/config/opal-credential/certificate?id=${encodeURIComponent(opalUrl)}`);
}

/**
 * The credentials Mica uses to connect to the Opal servers: a user name and password, a personal
 * access token or a key pair, by Opal URL.
 */
export function useOpalCredentials() {
  const credentials = ref<OpalCredentialDto[]>([]);
  const loading = ref(false);

  async function load(): Promise<OpalCredentialDto[]> {
    loading.value = true;
    try {
      const response = await api.get<OpalCredentialDto[]>('/config/opal-credentials');
      credentials.value = response.data;
    } catch (error) {
      notifyError(error);
      credentials.value = [];
    } finally {
      loading.value = false;
    }
    return credentials.value;
  }

  /** creates or updates the credential of the Opal URL */
  async function save(credential: OpalCredentialDto): Promise<boolean> {
    try {
      await api.post('/config/opal-credentials', credential);
      await load();
      return true;
    } catch (error) {
      notifyError(error);
      return false;
    }
  }

  async function remove(credential: Pick<OpalCredentialDto, 'opalUrl'>): Promise<boolean> {
    try {
      await api.delete('/config/opal-credential', { params: { id: credential.opalUrl } });
      await load();
      return true;
    } catch (error) {
      notifyError(error);
      return false;
    }
  }

  return { credentials, loading, load, save, remove };
}
