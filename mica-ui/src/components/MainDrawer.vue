<template>
  <div>
    <div v-if="authStore.isAuthenticated" class="q-mt-none q-mb-none q-pa-md">
      <span class="text-bold text-grey-6">{{ username }}</span>
    </div>
    <q-list>
      <q-item clickable @click="onProfile" v-if="authStore.isAuthenticated">
        <q-item-section avatar>
          <q-icon name="person" />
        </q-item-section>
        <q-item-section>
          <q-item-label>{{ t('my_profile') }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item clickable @click="onSignout" v-if="authStore.isAuthenticated">
        <q-item-section avatar>
          <q-icon name="logout" />
        </q-item-section>
        <q-item-section>
          <q-item-label>{{ t('auth.signout') }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-separator v-if="authStore.isAuthenticated" />
      <q-item to="/networks">
        <q-item-section avatar>
          <q-icon name="hub" />
        </q-item-section>
        <q-item-section>
          <q-item-label>{{ t('networks') }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item to="/studies">
        <q-item-section avatar>
          <q-icon name="book" />
        </q-item-section>
        <q-item-section>
          <q-item-label>{{ t('studies') }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item :to="`/datasets`">
        <q-item-section avatar>
          <q-icon name="splitscreen" />
        </q-item-section>
        <q-item-section>
          <q-item-label>{{ t('datasets') }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item :to="`/research-projects`">
        <q-item-section avatar>
          <q-icon name="science" />
        </q-item-section>
        <q-item-section>
          <q-item-label>{{ t('research_projects') }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item :to="`/files`">
        <q-item-section avatar>
          <q-icon name="folder" />
        </q-item-section>
        <q-item-section>
          <q-item-label>{{ t('files') }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item :to="`/persons`">
        <q-item-section avatar>
          <q-icon name="people" />
        </q-item-section>
        <q-item-section>
          <q-item-label>{{ t('persons') }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item v-if="authStore.isAdministrator" :to="`/settings`">
        <q-item-section avatar>
          <q-icon name="settings" />
        </q-item-section>
        <q-item-section>
          <q-item-label>{{ t('settings') }}</q-item-label>
        </q-item-section>
      </q-item>
      <q-item-label header>{{ t('other_links') }}</q-item-label>
      <EssentialLink v-for="link in essentialLinks" :key="link.title" v-bind="link" />
      <q-item class="fixed-bottom text-caption">
        <div>
          {{ t('main.powered_by') }}
          <a class="text-weight-bold" href="https://www.obiba.org/pages/products/mica2" target="_blank">OBiBa Mica</a>
          <span class="q-ml-xs" style="font-size: smaller">{{ authStore.version }}</span>
        </div>
      </q-item>
    </q-list>
  </div>
</template>

<script setup lang="ts">
import EssentialLink from 'components/EssentialLink.vue';
import type { EssentialLinkProps } from 'components/EssentialLink.vue';

const { t } = useI18n();
const authStore = useAuthStore();

const username = computed(() => authStore.session?.username || '?');

const essentialLinks: EssentialLinkProps[] = [
  {
    title: t('docs'),
    caption: t('documentation_cookbook'),
    icon: 'school',
    link: 'https://micadoc.obiba.org',
  },
  {
    title: t('source_code'),
    caption: 'github.com/obiba/mica2',
    icon: 'code',
    link: 'https://github.com/obiba/mica2',
  },
];

function onProfile() {
  window.location.href = '../profile';
}

function onSignout() {
  window.location.href = '../signout';
}
</script>
