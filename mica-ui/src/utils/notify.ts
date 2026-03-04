import { Notify } from 'quasar';

export function notifySuccess(message: string): void {
  Notify.create({
    type: 'positive',
    message,
    position: 'top-right',
  });
}

export function notifyError(message: string): void {
  Notify.create({
    type: 'negative',
    message,
    position: 'top-right',
  });
}

export function notifyWarning(message: string): void {
  Notify.create({
    type: 'warning',
    message,
    position: 'top-right',
  });
}

export function notifyInfo(message: string): void {
  Notify.create({
    type: 'info',
    message,
    position: 'top-right',
  });
}
