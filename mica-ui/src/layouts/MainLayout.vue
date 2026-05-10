<template>
  <q-layout v-show="authStore.isAuthenticated" view="lHh Lpr lFf">
    <q-header elevated class="bg-dark text-white">
      <q-toolbar>
        <q-btn flat dense round icon="menu" aria-label="Menu" @click="toggleLeftDrawer" />
        <q-btn flat to="/" no-caps size="lg">
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
import { contextPath } from 'src/boot/api';
import ReSigninDialog from 'src/components/ReSigninDialog.vue';
import MainDrawer from 'src/components/MainDrawer.vue';

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

onMounted(() => {
  router.beforeEach((to, from, next) => {
    if (to.path.startsWith('/admin') && !authStore.isAdministrator) {
      next('/');
    } else {
      next();
    }
  });
  authStore
    .userProfile()
    .then(() => {
      if (!authStore.isAdministrator) {
        router.push('/');
      }
    })
    .catch(() => {
      window.location.href = `..${contextPath === '/' ? '' : contextPath}/signin`;
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
  window.location.href = `..${contextPath === '/' ? '' : contextPath}/profile`;
}

function onSignout() {
  window.location.href = `..${contextPath === '/' ? '' : contextPath}/signout`;
}
</script>
