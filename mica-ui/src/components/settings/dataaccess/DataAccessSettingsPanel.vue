<template>
  <div>
    <q-spinner-dots v-if="state.loading && !form" color="primary" size="2em" />
    <div v-else-if="form">
      <div class="row q-col-gutter-xl">
        <div class="col-12 col-md-6">
          <div class="text-h6">{{ t('config.data_access.general') }}</div>
          <p class="text-grey-8">{{ t('config.data_access.general_info') }}</p>

          <div class="text-subtitle1 q-mt-md q-mb-sm">{{ t('permissions') }}</div>
          <config-toggle v-model="form.daoCanEdit" name="data_access.dao_can_edit" />

          <div class="text-subtitle1 q-mt-md q-mb-sm">{{ t('config.data_access.collaborators') }}</div>
          <config-toggle v-model="form.collaboratorsEnabled" name="data_access.collaborators_enabled" />
          <config-input
            v-model="form.collaboratorInvitationDays"
            name="data_access.collaborator_invitation_days"
            type="number"
            :min="1"
            :disable="!form.collaboratorsEnabled"
            class="q-ml-lg"
          />

          <div class="text-subtitle1 q-mt-md q-mb-sm">{{ t('config.data_access.other_forms') }}</div>
          <config-toggle v-model="form.preliminaryEnabled" name="data_access.preliminary_enabled" />
          <config-toggle
            v-model="form.mergePreliminaryContentEnabled"
            name="data_access.merge_preliminary_content_enabled"
            :disable="!form.preliminaryEnabled"
            class="q-ml-lg"
          />
          <config-toggle v-model="form.feasibilityEnabled" name="data_access.feasibility_enabled" />
          <config-toggle v-model="form.amendmentsEnabled" name="data_access.amendments_enabled" />
          <config-toggle v-model="form.agreementEnabled" name="data_access.agreement_enabled" />
          <q-select
            v-model="form.agreementOpenedPolicy"
            :options="agreementPolicies"
            :label="t('config.data_access.agreement_opened_policy')"
            :hint="t('config.data_access.agreement_opened_policy_help')"
            :disable="!form.agreementEnabled"
            emit-value
            map-options
            dense
            outlined
            class="q-ml-lg q-mb-md"
          />

          <div class="text-subtitle1 q-mt-md q-mb-sm">{{ t('config.data_access.variables') }}</div>
          <config-toggle v-model="form.variablesEnabled" name="data_access.variables_enabled" />
          <config-toggle v-model="form.preliminaryVariablesEnabled" name="data_access.preliminary_variables_enabled" />
          <config-toggle v-model="form.feasibilityVariablesEnabled" name="data_access.feasibility_variables_enabled" />
          <config-toggle v-model="form.amendmentVariablesEnabled" name="data_access.amendment_variables_enabled" />
        </div>

        <div class="col-12 col-md-6">
          <div class="text-h6">{{ t('config.data_access.id_generation') }}</div>
          <p class="text-grey-8">{{ t('config.data_access.id_generation_info') }}</p>
          <q-input
            v-model="form.idPrefix"
            :label="t('config.data_access.id_prefix')"
            :hint="t('config.data_access.id_prefix_help')"
            :rules="[(value) => !value || ID_PREFIX.test(value) || t('config.data_access.id_prefix_error')]"
            lazy-rules
            dense
            outlined
            class="q-mb-md"
          />
          <config-input v-model="form.idLength" name="data_access.id_length" type="number" :min="1" />
          <config-toggle v-model="form.randomId" name="data_access.random_id" />
          <config-toggle v-model="form.allowIdWithLeadingZeros" name="data_access.allow_id_with_leading_zeros" />

          <div class="text-h6 q-mt-lg">{{ t('config.data_access.workflow') }}</div>
          <p class="text-grey-8">{{ t('config.data_access.workflow_info') }}</p>
          <config-toggle v-model="form.withReview" name="data_access.with_review" />
          <config-toggle v-model="form.withConditionalApproval" name="data_access.with_conditional_approval" />
          <config-toggle v-model="form.approvedFinal" name="data_access.approved_final" />
          <config-toggle v-model="form.rejectedFinal" name="data_access.rejected_final" />

          <div class="text-h6 q-mt-lg">{{ t('config.data_access.predefined_actions') }}</div>
          <p class="text-grey-8">{{ t('config.data_access.predefined_actions_info') }}</p>
          <q-select
            v-model="form.predefinedActions"
            :label="t('config.data_access.predefined_actions')"
            :hint="t('config.data_access.predefined_actions_help', { prefix: ACTION_PREFIX })"
            use-input
            use-chips
            multiple
            hide-dropdown-icon
            new-value-mode="add-unique"
            input-debounce="0"
            dense
            outlined
            class="q-mb-md"
          />
        </div>
      </div>

      <div class="q-mt-md q-gutter-sm">
        <q-btn color="primary" :label="t('save')" :loading="state.saving" :disable="!dirty || !valid" @click="onSave" />
        <q-btn flat color="primary" :label="t('cancel')" :disable="!dirty || state.saving" @click="reset" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import ConfigInput from 'src/components/settings/general/ConfigInput.vue';
import ConfigToggle from 'src/components/settings/general/ConfigToggle.vue';
import { useConfirmLeave } from 'src/composables/useConfirmLeave';
import type { DataAccessConfigState } from 'src/composables/useDataAccessConfig';
import type { DataAccessConfigDto } from 'src/models/Mica';
import { notifySuccess } from 'src/utils/notify';

/** the characters of the ID prefix */
const ID_PREFIX = /^[a-zA-Z0-9_-]+$/;
/** the translation keys of the predefined action names start with it */
const ACTION_PREFIX = 'data-access-request.action-log.config.label.';
/** when the end user agreements are opened for edition */
const AGREEMENT_POLICIES = ['ALWAYS', 'PRELIMINARY_APPROVED', 'MAIN_APPROVED'];

interface Props {
  /** the data access configuration, shared with the notifications panel */
  state: DataAccessConfigState;
}

const props = defineProps<Props>();
const { t } = useI18n();

const state = reactive(props.state);
/** a working copy of the configuration, discarded on cancel */
const form = ref<DataAccessConfigDto>();

/** the configuration as saved: an empty prefix is none */
function normalized(config: DataAccessConfigDto): DataAccessConfigDto {
  const result = { ...config };
  if (!result.idPrefix?.trim()) delete result.idPrefix;
  return result;
}

const dirty = computed(
  () =>
    form.value !== undefined &&
    state.config !== undefined &&
    JSON.stringify(normalized(form.value)) !== JSON.stringify(normalized(state.config)),
);
const valid = computed(() => !form.value?.idPrefix || ID_PREFIX.test(form.value.idPrefix));
const { confirmLeave } = useConfirmLeave(dirty);

const agreementPolicies = computed(() =>
  AGREEMENT_POLICIES.map((policy) => ({
    value: policy,
    label: t(`config.data_access.agreement_opened_policy_${policy}`),
  })),
);

function reset() {
  form.value = state.config ? structuredClone(toRaw(state.config)) : undefined;
}

async function onSave() {
  if (!form.value) return;
  if (await state.save(normalized(form.value))) {
    notifySuccess(t('config.data_access.saved'));
    reset();
  }
}

watch(() => state.config, reset, { immediate: true });

defineExpose({ confirmLeave });
</script>
