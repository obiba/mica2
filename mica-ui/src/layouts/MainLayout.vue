<template>
  <q-layout v-show="authStore.isAuthenticated" view="lHh Lpr lFf">
    <q-header elevated class="bg-dark text-white">
      <q-toolbar>
        <q-btn flat dense round icon="menu" aria-label="Menu" @click="toggleLeftDrawer" />
        <q-btn flat type="a" :href="toPortalUrl('/')" no-caps size="lg">
          {{ appName }}
        </q-btn>
        <q-space />
        <div class="q-gutter-sm row items-center no-wrap">
          <q-btn-dropdown flat :label="locale">
            <q-list>
              <q-item
                clickable
                v-close-popup
                @click="onLocaleSelection(localeOpt)"
                v-for="localeOpt in localeOptions"
                :key="localeOpt.value"
              >
                <q-item-section>
                  <q-item-label>{{ localeOpt.label }}</q-item-label>
                </q-item-section>
                <q-item-section avatar v-if="locale === localeOpt.value">
                  <q-icon color="primary" name="check" />
                </q-item-section>
              </q-item>
            </q-list>
          </q-btn-dropdown>
          <q-btn-dropdown flat no-caps :label="username">
            <q-list>
              <q-item clickable v-close-popup @click="onProfile" v-if="authStore.isAuthenticated">
                <q-item-section>
                  <q-item-label>{{ t('my_profile') }}</q-item-label>
                </q-item-section>
              </q-item>
              <q-item clickable v-close-popup @click="onSignout" v-if="authStore.isAuthenticated">
                <q-item-section>
                  <q-item-label>{{ t('auth.signout') }}</q-item-label>
                </q-item-section>
              </q-item>
            </q-list>
          </q-btn-dropdown>
        </div>
      </q-toolbar>
    </q-header>

    <q-drawer v-model="leftDrawerOpen" show-if-above bordered>
      <main-drawer />
    </q-drawer>

    <q-page-container>
      <router-view />
      <re-signin-dialog v-model="authStore.reAuthRequired" />
    </q-page-container>
  </q-layout>
</template>

<script setup lang="ts">
import { Cookies } from 'quasar';
import { locales } from 'boot/i18n';
import { toPortalUrl } from 'src/boot/api';
import ReSigninDialog from 'src/components/ReSigninDialog.vue';
import MainDrawer from 'src/components/MainDrawer.vue';
import type { DocumentType } from 'src/composables/useDocumentTarget';
import { entityConfig } from 'src/utils/entityConfigs';
import type { RouteLocationNormalized } from 'vue-router';

const { t } = useI18n();
const router = useRouter();
const authStore = useAuthStore();
const systemStore = useSystemStore();

const leftDrawerOpen = ref(false);
const { locale } = useI18n({ useScope: 'global' });

const localeOptions = computed(() => {
  return locales.map((key) => ({
    label: key.toUpperCase(),
    value: key,
  }));
});
const appName = computed(() => systemStore.configurationPublic?.name || 'Agate');

// pages reserved to administrators
function isAdminPath(path: string) {
  return /^\/(settings|persons)(\/|$)/.test(path);
}

// pages of a section disabled in the Mica configuration, decided once the configuration is loaded
function isDisabledSection(route: RouteLocationNormalized) {
  const type = route.meta.documentType as DocumentType | undefined;
  const config = systemStore.configuration;
  if (Object.keys(config).length === 0) return false;
  if (route.path === '/settings/data-access') return !config.isDataAccessEnabled;
  return !!type && !entityConfig(type).isEnabled(config);
}

onMounted(() => {
  router.beforeEach((to, from, next) => {
    if ((authStore.isAuthenticated && !authStore.isAdministrator && isAdminPath(to.path)) || isDisabledSection(to)) {
      next('/');
    } else {
      next();
    }
  });
  authStore
    .userProfile()
    .then(() => {
      if (!authStore.isAdministrator && isAdminPath(router.currentRoute.value.path)) {
        router.replace('/');
      }
      systemStore.init().then(() => {
        if (isDisabledSection(router.currentRoute.value)) {
          router.replace('/');
        }
      });
    })
    .catch(() => {
      window.location.href = toPortalUrl('/signin');
    });
  systemStore.initPub();
});

const username = computed(() => authStore.session?.username || '?');

function toggleLeftDrawer() {
  leftDrawerOpen.value = !leftDrawerOpen.value;
}

function onLocaleSelection(localeOpt: { label: string; value: string }) {
  locale.value = localeOpt.value;
  Cookies.set('locale', localeOpt.value);
}

function onProfile() {
  window.location.href = toPortalUrl('/profile');
}

function onSignout() {
  window.location.href = toPortalUrl('/signout');
}
</script>
