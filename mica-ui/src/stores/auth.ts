import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { api } from 'boot/api';

interface Session {
  username: string;
  roles: string[];
}

export const useAuthStore = defineStore('auth', () => {
  const session = ref<Session | null>(null);

  const isAuthenticated = computed(() => session.value !== null);

  const isAdministrator = computed(
    () => session.value?.roles?.includes('mica-administrator') ?? false,
  );

  async function userProfile(): Promise<Session> {
    const response = await api.get<Session>('/auth/session/_current');
    session.value = response.data;
    return response.data;
  }

  async function signin(username: string, password: string): Promise<void> {
    await api.post('/auth/sessions', { username, password });
    await userProfile();
  }

  async function signout(): Promise<void> {
    try {
      await api.delete('/auth/session/_current');
    } finally {
      session.value = null;
    }
  }

  return {
    session,
    isAuthenticated,
    isAdministrator,
    userProfile,
    signin,
    signout,
  };
});
