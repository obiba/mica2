<template>
  <div>
    <div class="text-grey-8 q-mb-lg">{{ t('config.data_access.notifications_info') }}</div>
    <q-spinner-dots v-if="state.loading && !form" color="primary" size="2em" />
    <div v-else-if="form">
      <q-tabs
        v-model="tab"
        dense
        align="left"
        class="text-grey-7"
        active-color="primary"
        indicator-color="primary"
        narrow-indicator
      >
        <q-tab name="events" :label="t('config.data_access.events')" />
        <q-tab name="reports" :label="t('config.data_access.reports')" />
        <q-tab name="collaborators" :label="t('config.data_access.collaborators')" />
      </q-tabs>
      <q-separator />
      <q-tab-panels v-model="tab">
        <q-tab-panel name="events" class="q-px-none">
          <div class="row q-col-gutter-lg">
            <div v-for="(column, index) in EVENT_COLUMNS" :key="index" class="col-12 col-md-6">
              <template v-for="event in column" :key="event">
                <config-toggle v-model="form[notifyField(event)]" :name="`data_access.notify_${snake(event)}`" />
                <config-input
                  v-model="form[subjectField(event)]"
                  :name="`data_access.${snake(event)}_subject`"
                  :placeholder="SUBJECT_PLACEHOLDER"
                  :disable="!form[notifyField(event)]"
                  no-help
                />
              </template>
            </div>
          </div>
        </q-tab-panel>
        <q-tab-panel name="reports" class="q-px-none">
          <div class="row q-col-gutter-lg">
            <div class="col-12 col-md-6">
              <template v-for="event in REPORT_EVENTS" :key="event">
                <config-toggle v-model="form[notifyField(event)]" :name="`data_access.notify_${snake(event)}`" />
                <config-input
                  v-model="form[subjectField(event)]"
                  :name="`data_access.${snake(event)}_subject`"
                  :placeholder="SUBJECT_PLACEHOLDER"
                  :disable="!form[notifyField(event)]"
                  no-help
                />
              </template>
            </div>
            <div class="col-12 col-md-6">
              <config-input
                v-model="form.nbOfDaysBeforeReport"
                name="data_access.nb_of_days_before_report"
                type="number"
                :min="0"
              />
            </div>
          </div>
        </q-tab-panel>
        <q-tab-panel name="collaborators" class="q-px-none">
          <div class="row q-col-gutter-lg">
            <div class="col-12 col-md-6">
              <div v-if="!form.collaboratorsEnabled" class="text-grey-7 q-mb-lg">
                {{ t('config.data_access.collaborators_disabled') }}
              </div>
              <config-input
                v-model="form.collaboratorInvitationSubject"
                name="data_access.collaborator_invitation_subject"
                :placeholder="SUBJECT_PLACEHOLDER"
                :disable="!form.collaboratorsEnabled"
                no-help
              />
              <config-toggle
                v-model="form.notifyCollaboratorAccepted"
                name="data_access.notify_collaborator_accepted"
                :disable="!form.collaboratorsEnabled"
              />
              <config-input
                v-model="form.collaboratorAcceptedSubject"
                name="data_access.collaborator_accepted_subject"
                :placeholder="SUBJECT_PLACEHOLDER"
                :disable="!form.collaboratorsEnabled || !form.notifyCollaboratorAccepted"
                no-help
              />
            </div>
          </div>
        </q-tab-panel>
      </q-tab-panels>
      <div class="q-mt-md q-gutter-sm">
        <q-btn color="primary" :label="t('save')" :loading="state.saving" :disable="!dirty" @click="save" />
        <q-btn flat color="primary" :label="t('cancel')" :disable="!dirty || state.saving" @click="reset" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import ConfigInput from 'src/components/settings/general/ConfigInput.vue';
import ConfigToggle from 'src/components/settings/general/ConfigToggle.vue';
import { useWorkingCopy } from 'src/composables/useWorkingCopy';
import type { DataAccessConfigState } from 'src/composables/useDataAccessConfig';
import type { DataAccessConfigDto } from 'src/models/Mica';
import { notifySuccess } from 'src/utils/notify';

/** the events of a request notified by email: `notify<Event>` toggles, `<event>Subject` the subject */
const EVENT_COLUMNS = [
  ['created', 'submitted', 'reviewed', 'reopened', 'conditionallyApproved'],
  ['rejected', 'approved', 'commented', 'attachment'],
] as const;
const REPORT_EVENTS = ['finalReport', 'intermediateReport'] as const;
type NotifiedEvent = (typeof EVENT_COLUMNS)[number][number] | (typeof REPORT_EVENTS)[number];
type NotifyField = `notify${Capitalize<NotifiedEvent>}`;
type SubjectField = `${NotifiedEvent}Subject`;

/** the default subject of the emails, as the server builds it */
const SUBJECT_PLACEHOLDER = '[${organization}] ${title}';

interface Props {
  /** the data access configuration, shared with the other settings panel */
  state: DataAccessConfigState;
}

const props = defineProps<Props>();
const { t } = useI18n();

const state = reactive(props.state);
const tab = ref('events');
/** a working copy of the configuration, discarded on cancel */
const { form, dirty, reset, save, confirmLeave } = useWorkingCopy<DataAccessConfigDto>({
  source: () => state.config,
  save: state.save,
  onSaved: () => notifySuccess(t('config.data_access.saved')),
});

function notifyField(event: NotifiedEvent): NotifyField {
  return `notify${event.charAt(0).toUpperCase()}${event.slice(1)}` as NotifyField;
}

function subjectField(event: NotifiedEvent): SubjectField {
  return `${event}Subject`;
}

/** `conditionallyApproved` as `conditionally_approved`, the message key */
function snake(event: string): string {
  return event.replace(/[A-Z]/g, (letter) => `_${letter.toLowerCase()}`);
}

defineExpose({ confirmLeave });
</script>
