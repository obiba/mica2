import { Notify } from 'quasar';
import { t } from 'src/boot/i18n';

export function notifySuccess(message: string) {
  Notify.create({
    type: 'positive',
    message: t(message),
  });
}

export function notifyInfo(message: string) {
  Notify.create({
    type: 'info',
    message: t(message),
  });
}

export function notifyWarning(message: string) {
  Notify.create({
    type: 'warning',
    message: t(message),
  });
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function notifyError(error: any) {
  let message = t('unknown_error');
  if (typeof error === 'string') {
    message = t(error);
  } else {
    console.error(error);
    message = error.message;
    if (error.response?.data && error.response.data?.code) {
      message = t(`error.${error.response?.data.code}`);
      if (error.response.data.messageTemplate) {
        message = t(error.response.data.messageTemplate, error.response.data.arguments);
      } else if (error.response.data.message) {
        message = error.response.data.message;
      }
    }
  }
  Notify.create({
    type: isReAuthError(error) ? 'warning' : 'negative',
    message,
  });
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function isReAuthError(error: any): boolean {
  if (error.response?.status === 401) {
    if (error.response.data.messageTemplate === 'server.error.reauthentication_required') {
      return true;
    }
  }
  return false;
}
