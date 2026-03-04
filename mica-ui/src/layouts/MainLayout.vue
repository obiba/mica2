<template>
  <q-layout view="lHh Lpr lFf">
    <q-header elevated class="bg-dark text-white">
      <q-toolbar>
        <q-btn
          flat
          dense
          round
          icon="menu"
          aria-label="Menu"
          @click="toggleLeftDrawer"
        />
        <q-toolbar-title>
          {{ $t('app.name') }}
        </q-toolbar-title>

        <!-- Locale switcher -->
        <q-btn-dropdown flat :label="locale" dense>
          <q-list>
            <q-item
              v-for="lang in supportedLocales"
              :key="lang"
              v-close-popup
              clickable
              @click="setLocale(lang)"
            >
              <q-item-section>
                <q-item-label>{{ lang.toUpperCase() }}</q-item-label>
              </q-item-section>
            </q-item>
          </q-list>
        </q-btn-dropdown>

        <!-- User dropdown -->
        <q-btn-dropdown flat :label="authStore.session?.username || ''" dense icon="person">
          <q-list>
            <q-item v-close-popup clickable @click="signout">
              <q-item-section>
                <q-item-label>{{ $t('auth.signout') }}</q-item-label>
              </q-item-section>
            </q-item>
          </q-list>
        </q-btn-dropdown>
      </q-toolbar>
    </q-header>

    <q-drawer v-model="leftDrawerOpen" show-if-above bordered>
      <q-list>
        <q-item-label header>{{ $t('nav.menu') }}</q-item-label>
        <EssentialLink
          v-for="link in linksList"
          :key="link.title"
          v-bind="link"
        />
      </q-list>
    </q-drawer>

    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { Cookies } from 'quasar';
import EssentialLink, { type EssentialLinkProps } from 'components/EssentialLink.vue';
import { useAuthStore } from 'stores/auth';

const SUPPORTED_LOCALES = ['en', 'fr'];

const linksList: EssentialLinkProps[] = [
  {
    title: 'nav.home',
    icon: 'home',
    to: '/',
  },
];

const leftDrawerOpen = ref(false);
const { locale } = useI18n({ useScope: 'global' });
const authStore = useAuthStore();

const supportedLocales = SUPPORTED_LOCALES;

function toggleLeftDrawer() {
  leftDrawerOpen.value = !leftDrawerOpen.value;
}

function setLocale(lang: string) {
  locale.value = lang;
  Cookies.set('locale', lang, { path: '/' });
}

async function signout() {
  await authStore.signout();
  const contextPath = (() => {
    const path = window.location.pathname;
    const idx = path.indexOf('/mica-ui');
    return idx >= 0 ? path.substring(0, idx) || '/' : '/';
  })();
  window.location.assign(contextPath + '/signin');
}

onMounted(async () => {
  try {
    await authStore.userProfile();
  } catch {
    const contextPath = (() => {
      const path = window.location.pathname;
      const idx = path.indexOf('/mica-ui');
      return idx >= 0 ? path.substring(0, idx) || '/' : '/';
    })();
    window.location.assign(contextPath + '/signin');
  }
});
</script>
