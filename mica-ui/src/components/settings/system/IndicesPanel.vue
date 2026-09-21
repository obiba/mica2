<template>
  <div>
    <q-banner v-if="warning" rounded class="bg-warning text-dark q-mb-lg">
      <template v-slot:avatar>
        <q-icon name="warning" />
      </template>
      {{ t(warning) }}
      <template v-slot:action>
        <q-btn flat :label="t('close')" @click="warning = undefined" />
      </template>
    </q-banner>
    <q-card flat bordered class="q-mb-lg">
      <q-item class="q-py-md">
        <q-item-section avatar>
          <q-avatar icon="manage_search" color="primary" text-color="white" />
        </q-item-section>
        <q-item-section>
          <q-item-label class="text-subtitle1">{{ t('config.indexing.rebuild_all') }}</q-item-label>
          <q-item-label caption>{{ t('config.indexing.rebuild_all_help') }}</q-item-label>
        </q-item-section>
        <q-item-section side>
          <q-btn
            color="primary"
            icon="play_arrow"
            :label="t('config.indexing.rebuild')"
            size="sm"
            :loading="busy === 'all'"
            :disable="busy !== undefined"
            @click="onRebuildAll"
          />
        </q-item-section>
      </q-item>
    </q-card>
    <q-card flat bordered>
      <q-list separator>
        <system-action-item
          v-for="index in indices"
          :key="index.key"
          :title="t(`config.indexing.indices.${index.key}`)"
          :help="t(`config.indexing.indices.${index.key}_help`)"
          :icon="index.icon"
          :busy="busy === index.key"
        >
          <q-btn
            flat
            dense
            size="sm"
            icon="play_arrow"
            color="primary"
            :label="t('config.indexing.rebuild')"
            :disable="busy !== undefined"
            @click="onRebuild(index)"
          />
        </system-action-item>
      </q-list>
    </q-card>
    <confirm-dialog
      v-model="showConfirm"
      :title="t('config.indexing.rebuild_title')"
      :text="confirmText"
      @confirm="onConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import ConfirmDialog from 'src/components/ConfirmDialog.vue';
import SystemActionItem from 'src/components/settings/system/SystemActionItem.vue';
import { ALL_INDICES_WARNING, enabledIndices, useIndexing, type IndexInfo } from 'src/composables/useIndexing';
import { notifySuccess } from 'src/utils/notify';

const { t } = useI18n();
const systemStore = useSystemStore();
const { busy, rebuildAll, rebuild } = useIndexing();

const indices = computed(() => enabledIndices(systemStore.configuration));

const showConfirm = ref(false);
/** the index to rebuild on confirmation, none for all of them */
const target = ref<IndexInfo>();
/** the reminder shown after a rebuild, when the studies annotations may be affected */
const warning = ref<string>();

const confirmText = computed(() =>
  target.value
    ? t('config.indexing.rebuild_text', { name: t(`config.indexing.indices.${target.value.key}`) })
    : t('config.indexing.rebuild_all_text'),
);

function onRebuildAll() {
  target.value = undefined;
  showConfirm.value = true;
}

function onRebuild(index: IndexInfo) {
  target.value = index;
  showConfirm.value = true;
}

async function onConfirm() {
  const index = target.value;
  if (index) {
    if (await rebuild(index)) {
      notifySuccess(t('config.indexing.started'));
      warning.value = index.warning;
    }
  } else if (await rebuildAll()) {
    notifySuccess(t('config.indexing.all_started'));
    warning.value = ALL_INDICES_WARNING;
  }
}
</script>
