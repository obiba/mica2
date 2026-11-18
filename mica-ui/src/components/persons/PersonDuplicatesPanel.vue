<template>
  <div>
    <div class="text-caption text-grey-7 q-mb-sm">{{ t(`persons.duplicates_help_${by}`) }}</div>
    <q-inner-loading :showing="loading" />
    <div v-if="!loading && groups.length === 0" class="text-grey-7">{{ t('persons.duplicates_none') }}</div>
    <q-markup-table v-else-if="groups.length > 0" flat dense separator="horizontal">
      <thead>
        <tr>
          <th class="text-left">{{ t('name') }}</th>
          <th class="text-left">{{ t('email') }}</th>
          <th class="text-left">{{ t('persons.created') }}</th>
          <th class="text-left">{{ t('last_modified') }}</th>
          <th v-for="kind in MEMBERSHIP_KINDS" :key="kind" class="text-left">
            {{ t(MEMBERSHIP_INFO[kind].title) }}
          </th>
        </tr>
      </thead>
      <tbody v-for="(group, index) in groups" :key="index">
        <tr class="bg-grey-2">
          <td :colspan="4 + MEMBERSHIP_KINDS.length" class="text-weight-medium">
            {{ t('persons.duplicates_group', { name: fullName(group[0]), count: group.length }) }}
          </td>
        </tr>
        <tr v-for="person in group" :key="person.id">
          <td>
            <router-link :to="`/persons/${person.id}`" class="text-primary">{{ fullName(person) }}</router-link>
          </td>
          <td>{{ person.email }}</td>
          <td>{{ getDateLabel(person.timestamps?.created) }}</td>
          <td>{{ getDateLabel(person.timestamps?.lastUpdate) }}</td>
          <td v-for="kind in MEMBERSHIP_KINDS" :key="kind">
            <div v-for="entity in groupMemberships(person, kind, locale)" :key="entity.id">
              <router-link :to="entity.route" class="text-primary">{{ entity.acronym }}</router-link>
              <span class="text-grey-7 q-ml-xs">({{ entity.roles.map(roleLabel).join(', ') }})</span>
            </div>
          </td>
        </tr>
      </tbody>
    </q-markup-table>
  </div>
</template>

<script setup lang="ts">
import type { PersonDto } from 'src/models/Mica';
import { getDateLabel } from 'src/utils/dates';
import { notifyError } from 'src/utils/notify';
import { fullName, groupMemberships, MEMBERSHIP_INFO, MEMBERSHIP_KINDS } from 'src/utils/persons';
import { usePersonsStore } from 'src/stores/persons';
import { useRoleLabels } from 'src/composables/useRoleLabels';

const props = defineProps<{ by: 'identical' | 'name' }>();

const personsStore = usePersonsStore();
const { roleLabel } = useRoleLabels();
const { t, locale } = useI18n();

const loading = ref(false);
const groups = ref<PersonDto[][]>([]);

async function load() {
  loading.value = true;
  try {
    groups.value = await personsStore.duplicateGroups(props.by);
  } catch (error) {
    groups.value = [];
    notifyError(error);
  } finally {
    loading.value = false;
  }
}

watch(() => props.by, load, { immediate: true });

defineExpose({ load });
</script>
