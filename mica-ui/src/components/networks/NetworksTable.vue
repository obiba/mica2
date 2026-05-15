<template>
  <div>
    <q-table flat :rows="networksStore.networks" :columns="columns" row-key="id">
      <template v-slot:body-cell-id="props">
        <q-td key="id" :props="props">
          <router-link :to="`/network/${props.value}`" class="text-primary">{{ props.value }}</router-link>
        </q-td>
      </template>
      <template v-slot:body-cell-name="props">
        <q-td key="name" :props="props">
          <div v-for="name in props.row.name" :key="name">
            <q-badge color="primary" class="q-mr-sm">{{ name.lang }}</q-badge>
            {{ name.value }}
          </div>
        </q-td>
      </template>
    </q-table>
  </div>
</template>

<script setup lang="ts">
import type { TimestampsDto } from 'src/models/Mica';

const networksStore = useNetworksStore();
const { t } = useI18n();

const columns = computed(() => [
  { name: 'id', label: 'ID', field: 'id', sortable: true },
  { name: 'name', label: t('name'), field: 'name', sortable: true },
  {
    name: 'lastUpdated',
    label: t('lastUpdated'),
    field: 'timestamps',
    format: (ts: TimestampsDto) => ts.lastUpdate,
    sortable: true,
  },
]);

onMounted(() => {
  networksStore.fetchNetworks();
});
</script>
