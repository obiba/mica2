<template>
  <q-dialog v-model="show" @before-show="onShow">
    <q-card class="dialog-md">
      <q-card-section>
        <div class="text-h6">{{ title }}</div>
        <div v-if="hint" class="text-hint">{{ hint }}</div>
      </q-card-section>
      <q-separator />
      <slot />
      <q-card-section :class="{ 'q-pt-none': !!$slots.default }">
        <q-table
          v-model:selected="selected"
          flat
          dense
          :rows="rows"
          :columns="columns"
          row-key="id"
          selection="multiple"
          :loading="loading"
          :filter="filter"
          :pagination="{ rowsPerPage: 10 }"
          :rows-per-page-options="[10, 25, 50]"
          :no-data-label="noDataLabel"
        >
          <template v-slot:top-right>
            <q-input v-model="filter" dense debounce="300" :placeholder="t('search')">
              <template v-slot:append>
                <q-icon name="search" />
              </template>
            </q-input>
          </template>
        </q-table>
      </q-card-section>
      <q-separator />
      <q-card-actions align="right" class="bg-grey-3">
        <q-btn flat :label="t('cancel')" color="secondary" v-close-popup />
        <q-btn
          flat
          :label="t('add')"
          color="primary"
          :disable="disable || selected.length === 0"
          v-close-popup
          @click="onAdd"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import type { QTableColumn } from 'quasar';
import type { LocalizedStringDto } from 'src/models/Mica';
import { documentTarget, type DocumentType } from 'src/composables/useDocumentTarget';
import { notifyError } from 'src/utils/notify';
import { localized, type MembershipParent } from 'src/utils/persons';

interface Props {
  /** the type of the documents to select */
  type: DocumentType;
  /** the ids of the documents left out */
  exclude: string[];
  title: string;
  hint?: string | undefined;
  noDataLabel?: string | undefined;
  /** the selection cannot be added yet */
  disable?: boolean;
}

/** a document that can be selected */
interface Row extends MembershipParent {
  acronymLabel: string;
  nameLabel: string;
}

const props = defineProps<Props>();
const show = defineModel<boolean>({ required: true });
const emit = defineEmits<{ select: [documents: MembershipParent[]] }>();
const { t, locale } = useI18n();
const documentsStore = useDocumentsStore();

const columns = computed<QTableColumn[]>(() => [
  { name: 'acronym', label: t('acronym'), field: 'acronymLabel', align: 'left', sortable: true },
  { name: 'name', label: t('name'), field: 'nameLabel', align: 'left', sortable: true, style: 'white-space: normal' },
]);

const loading = ref(false);
const rows = ref<Row[]>([]);
const selected = ref<Row[]>([]);
const filter = ref('');

async function onShow() {
  selected.value = [];
  filter.value = '';
  loading.value = true;
  try {
    const excluded = new Set(props.exclude);
    const list = await documentsStore.fetchDocuments(documentTarget(props.type, ''));
    rows.value = list
      .filter((entity) => entity.id && !excluded.has(entity.id))
      .map((entity) => {
        const acronym = (entity as { acronym?: LocalizedStringDto[] }).acronym ?? [];
        const name = entity.name ?? [];
        return {
          id: entity.id as string,
          acronym,
          name,
          acronymLabel: localized(acronym, locale.value),
          nameLabel: localized(name, locale.value),
        };
      })
      .sort((a, b) => a.acronymLabel.localeCompare(b.acronymLabel));
  } catch (error) {
    rows.value = [];
    notifyError(error);
  } finally {
    loading.value = false;
  }
}

function onAdd() {
  emit(
    'select',
    selected.value.map(({ id, acronym, name }) => ({ id, acronym, name })),
  );
}
</script>
