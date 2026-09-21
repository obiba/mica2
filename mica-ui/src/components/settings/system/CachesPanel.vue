<template>
  <div>
    <q-card flat bordered class="q-mb-lg">
      <q-item class="q-py-md">
        <q-item-section avatar>
          <q-avatar icon="cleaning_services" color="primary" text-color="white" />
        </q-item-section>
        <q-item-section>
          <q-item-label class="text-subtitle1">{{ t('config.caching.clear_all') }}</q-item-label>
          <q-item-label caption>{{ t('config.caching.clear_all_help') }}</q-item-label>
        </q-item-section>
        <q-item-section side>
          <q-btn
            color="primary"
            icon="delete_sweep"
            :label="t('config.caching.clear')"
            size="sm"
            :loading="busy === 'all'"
            :disable="busy !== undefined"
            @click="onClearAll"
          />
        </q-item-section>
      </q-item>
    </q-card>
    <q-card flat bordered>
      <q-list separator>
        <system-action-item
          v-for="id in CACHE_IDS"
          :key="id"
          :title="t(`config.caching.caches.${id}`)"
          :help="t(`config.caching.caches.${id}_help`)"
          :icon="ICONS[id]"
          :busy="busy === id"
        >
          <q-btn
            v-if="BUILDABLE_CACHE_IDS.includes(id)"
            flat
            dense
            size="sm"
            icon="build"
            color="primary"
            :label="t('config.caching.build')"
            :disable="busy !== undefined"
            @click="onBuild(id)"
          />
          <q-btn
            flat
            dense
            size="sm"
            icon="delete"
            color="negative"
            :label="t('config.caching.clear')"
            :disable="busy !== undefined"
            @click="onClear(id)"
          />
        </system-action-item>
      </q-list>
    </q-card>
    <confirm-dialog
      v-model="showConfirm"
      :title="confirmTitle"
      :text="confirmText"
      @confirm="onConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import ConfirmDialog from 'src/components/ConfirmDialog.vue';
import SystemActionItem from 'src/components/settings/system/SystemActionItem.vue';
import { BUILDABLE_CACHE_IDS, CACHE_IDS, useCaches, type CacheId } from 'src/composables/useCaches';
import { notifySuccess } from 'src/utils/notify';

const { t } = useI18n();
const { busy, clearAll, clear, build } = useCaches();

const ICONS: Record<CacheId, string> = {
  micaConfig: 'settings',
  variableTaxonomies: 'account_tree',
  aggregationsMetadata: 'label',
  datasetVariables: 'bar_chart',
  authorization: 'lock',
};

type Action = { kind: 'clearAll' } | { kind: 'clear' | 'build'; id: CacheId };

const showConfirm = ref(false);
const action = ref<Action>();

const confirmTitle = computed(() => {
  if (!action.value) return '';
  return t(action.value.kind === 'build' ? 'config.caching.build_title' : 'config.caching.clear_title');
});

const confirmText = computed(() => {
  if (!action.value) return '';
  if (action.value.kind === 'clearAll') return t('config.caching.clear_all_text');
  const name = t(`config.caching.caches.${action.value.id}`);
  return t(action.value.kind === 'build' ? 'config.caching.build_text' : 'config.caching.clear_text', { name });
});

function ask(next: Action) {
  action.value = next;
  showConfirm.value = true;
}

const onClearAll = () => ask({ kind: 'clearAll' });
const onClear = (id: CacheId) => ask({ kind: 'clear', id });
const onBuild = (id: CacheId) => ask({ kind: 'build', id });

async function onConfirm() {
  const current = action.value;
  if (!current) return;
  if (current.kind === 'clearAll') {
    if (await clearAll()) notifySuccess(t('config.caching.all_cleared'));
  } else if (current.kind === 'clear') {
    if (await clear(current.id)) notifySuccess(t('config.caching.cleared'));
  } else {
    if (await build(current.id)) notifySuccess(t('config.caching.built'));
  }
}
</script>
