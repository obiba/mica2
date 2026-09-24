<template>
  <div>
    <div class="row items-center q-gutter-sm q-mb-md">
      <q-btn
        v-if="canEdit"
        color="primary"
        icon="add"
        :label="t('add')"
        size="sm"
        :disable="busy"
        @click="showAdd = true"
      />
      <q-btn
        v-if="canEdit"
        color="negative"
        icon="delete"
        :label="t('network_links.remove_selected')"
        size="sm"
        :disable="busy || selected.length === 0"
        @click="toRemove = selected"
      />
      <q-btn
        v-if="reportUrl"
        color="secondary"
        icon="download"
        :label="t('network_links.report')"
        size="sm"
        :href="reportUrl"
        download
      />
    </div>
    <q-table
      v-model:selected="selected"
      flat
      dense
      :rows="links"
      :columns="columns"
      row-key="id"
      :selection="canEdit ? 'multiple' : 'none'"
      :pagination="{ rowsPerPage: 10 }"
      :rows-per-page-options="[10, 25, 50]"
      :no-data-label="t(`network_links.none.${kind}`)"
      :hide-pagination="links.length <= 10"
    >
      <template v-slot:body-cell-acronym="props">
        <q-td :props="props">
          <router-link v-if="props.row.route" :to="props.row.route" class="text-primary">{{ props.value }}</router-link>
          <span v-else>{{ props.value }}</span>
        </q-td>
      </template>
      <template v-slot:body-cell-published="props">
        <q-td :props="props">
          <q-icon :name="props.value ? 'star' : 'star_border'" size="sm" />
        </q-td>
      </template>
      <template v-slot:body-cell-actions="props">
        <q-td :props="props">
          <q-btn
            v-if="canEdit"
            flat
            dense
            size="sm"
            icon="delete"
            color="negative"
            :title="t('delete')"
            :disable="busy"
            @click="toRemove = [props.row]"
          />
        </q-td>
      </template>
    </q-table>

    <select-documents-dialog
      v-model="showAdd"
      :type="kind"
      :exclude="linkedIds(network, kind)"
      :title="`${t('network_links.add_title')}: ${t(documentTarget(kind, '').labels.title)}`"
      :no-data-label="t('network_links.all_linked')"
      @select="onAdd"
    />

    <confirm-dialog
      :model-value="toRemove.length > 0"
      :title="t('network_links.remove_title')"
      :text="t('network_links.remove_text', { names: toRemove.map((link) => link.acronym).join(', ') })"
      @update:model-value="toRemove = []"
      @confirm="onRemove"
    />
  </div>
</template>

<script setup lang="ts">
import type { QTableColumn } from 'quasar';
import type { NetworkDto } from 'src/models/Mica';
import { toServerUrl } from 'src/boot/api';
import ConfirmDialog from 'src/components/ConfirmDialog.vue';
import SelectDocumentsDialog from 'src/components/documents/SelectDocumentsDialog.vue';
import { documentTarget } from 'src/composables/useDocumentTarget';
import {
  addLinks,
  linkedIds,
  networkLinks,
  removeLinks,
  type NetworkLink,
  type NetworkLinkKind,
} from 'src/utils/networks';
import type { MembershipParent } from 'src/utils/persons';

interface Props {
  network: NetworkDto;
  kind: NetworkLinkKind;
  canEdit: boolean;
  /** a change is being saved */
  busy?: boolean;
}

const props = defineProps<Props>();
/** the network with the links changed, to be saved */
const emit = defineEmits<{ change: [network: NetworkDto] }>();
const { t, locale } = useI18n();

const links = computed(() => networkLinks(props.network, props.kind, locale.value));
const columns = computed<QTableColumn[]>(() => [
  { name: 'acronym', label: t('acronym'), field: 'acronym', align: 'left', sortable: true },
  { name: 'name', label: t('name'), field: 'name', align: 'left', sortable: true, style: 'white-space: normal' },
  { name: 'published', label: t('network_links.published'), field: 'published', align: 'center' },
  { name: 'actions', label: '', field: 'id', align: 'right' },
]);

/** the CSV report of the published studies of the network */
const reportUrl = computed(() => {
  if (props.kind === 'network' || links.value.length === 0) return undefined;
  const params = new URLSearchParams({
    networkId: props.network.id ?? '',
    locale: locale.value,
    studyType: props.kind,
  });
  return toServerUrl(`/studies/_report_by_network?${params}`);
});

const showAdd = ref(false);
const selected = ref<NetworkLink[]>([]);
const toRemove = ref<NetworkLink[]>([]);

// the selection is of the links shown
watch(links, () => {
  selected.value = [];
});

function onAdd(documents: MembershipParent[]) {
  emit(
    'change',
    addLinks(
      props.network,
      props.kind,
      documents.map((document) => document.id),
    ),
  );
}

function onRemove() {
  emit(
    'change',
    removeLinks(
      props.network,
      props.kind,
      toRemove.value.map((link) => link.id),
    ),
  );
}
</script>
