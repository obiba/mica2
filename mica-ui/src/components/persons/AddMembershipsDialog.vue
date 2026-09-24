<template>
  <select-documents-dialog
    v-model="show"
    :type="MEMBERSHIP_INFO[kind].documentType"
    :exclude="members"
    :title="`${t('persons.add_memberships_title')}: ${t(MEMBERSHIP_INFO[kind].title)}`"
    :hint="t('persons.add_memberships_help')"
    :no-data-label="t('persons.member_of_all', { name: fullName(person) })"
    :disable="roles.length === 0"
    @select="onAdd"
  >
    <q-card-section>
      <div class="text-subtitle2">{{ t('persons.roles') }}</div>
      <q-option-group v-model="roles" :options="roleOptions" type="checkbox" inline />
    </q-card-section>
  </select-documents-dialog>
</template>

<script setup lang="ts">
import type { PersonDto } from 'src/models/Mica';
import SelectDocumentsDialog from 'src/components/documents/SelectDocumentsDialog.vue';
import {
  fullName,
  groupMemberships,
  MEMBERSHIP_INFO,
  type MembershipKind,
  type MembershipParent,
} from 'src/utils/persons';

interface Props {
  person: PersonDto;
  kind: MembershipKind;
}

const props = defineProps<Props>();
const show = defineModel<boolean>({ required: true });
const emit = defineEmits<{ add: [parents: MembershipParent[], roles: string[]] }>();
const { t, locale } = useI18n();
const systemStore = useSystemStore();

const roleOptions = computed(() =>
  (systemStore.configuration.roles ?? []).map((role) => ({ label: role, value: role })),
);
/** the entities of the kind the person is member of, not to be selected */
const members = computed(() => groupMemberships(props.person, props.kind, locale.value).map((entity) => entity.id));
const roles = ref<string[]>([]);

watch(show, (value) => {
  if (value) roles.value = [];
});

function onAdd(parents: MembershipParent[]) {
  emit('add', parents, roles.value);
}
</script>
