<template>
  <div>
    <q-toolbar class="bg-grey-3">
      <q-breadcrumbs>
        <q-breadcrumbs-el icon="home" to="/" />
        <q-breadcrumbs-el :label="t('files.title')" />
      </q-breadcrumbs>
    </q-toolbar>
    <q-page padding>
      <file-browser root="/" :path="filePath" @update:path="onFilePath" />
    </q-page>
  </div>
</template>

<script setup lang="ts">
import FileBrowser from 'src/components/files/FileBrowser.vue';

const route = useRoute();
const router = useRouter();
const { t } = useI18n();

/** the folder opened, kept in the route */
const filePath = computed(() => (typeof route.query.path === 'string' ? route.query.path : undefined));

function onFilePath(value: string) {
  router.replace({ query: { ...route.query, path: value === '/' ? undefined : value } });
}
</script>
