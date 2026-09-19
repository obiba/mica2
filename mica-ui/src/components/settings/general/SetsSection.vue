<template>
  <config-section :title="t('config.sets')" :items="items">
    <template #form="{ form }">
      <config-toggle v-model="form.isCartEnabled" name="cart_enabled" />
      <config-toggle v-model="form.anonymousCanCreateCart" name="anonymous_create_cart" />
      <config-toggle v-model="form.isStudiesCartEnabled" name="studies_cart_enabled" />
      <config-toggle v-model="form.isNetworksCartEnabled" name="networks_cart_enabled" />
      <config-toggle v-model="form.isSetsSearchEnabled" name="sets_search_enabled" />
      <config-input v-model="form.maxNumberOfSets" name="max_number_sets" type="number" :min="0" required />
      <config-input v-model="form.maxItemsPerSet" name="max_items_per_set" type="number" :min="0" required />
      <config-input
        v-model="form.cartTimeToLive"
        name="cart_time_to_live"
        type="number"
        :min="1"
        required
        :disable="!isAnyCartEnabled(form)"
      />
      <config-input v-model="form.setTimeToLive" name="set_time_to_live" type="number" :min="1" required />
      <q-select
        v-model="form.opalViewsGrouping"
        :options="groupingOptions"
        emit-value
        map-options
        dense
        outlined
        :label="t('config.opal_views_grouping') + ' *'"
        :hint="form.opalViewsGrouping ? t(`config.opal_views_grouping_help.${form.opalViewsGrouping}`) : undefined"
        :rules="[(value: string) => !!value || t('required')]"
        lazy-rules
        class="q-mb-md"
      />
    </template>
  </config-section>
</template>

<script setup lang="ts">
import ConfigSection from 'src/components/settings/general/ConfigSection.vue';
import ConfigInput from 'src/components/settings/general/ConfigInput.vue';
import ConfigToggle from 'src/components/settings/general/ConfigToggle.vue';
import { flagItem, valueItem } from 'src/components/settings/general/fields';
import type { FieldItem } from 'src/components/FieldsList.vue';
import type { MicaConfigDto } from 'src/models/Mica';
import { OPAL_VIEWS_GROUPINGS, isAnyCartEnabled } from 'src/utils/config';

const { t } = useI18n();
const systemStore = useSystemStore();

const groupingOptions = computed(() =>
  OPAL_VIEWS_GROUPINGS.map((grouping) => ({
    label: t(`config.opal_views_grouping_label.${grouping}`),
    value: grouping,
  })),
);

/** the hint of the grouping is the one of the current value */
const groupingItem = computed<FieldItem>(() => {
  const grouping = systemStore.configuration.opalViewsGrouping;
  return {
    field: 'opalViewsGrouping',
    label: 'config.opal_views_grouping',
    ...(grouping ? { hint: `config.opal_views_grouping_help.${grouping}` } : {}),
    format: (config: MicaConfigDto) =>
      config.opalViewsGrouping ? t(`config.opal_views_grouping_label.${config.opalViewsGrouping}`) : '-',
  };
});

const items = computed(() => [
  flagItem('isCartEnabled', 'cart_enabled'),
  flagItem('anonymousCanCreateCart', 'anonymous_create_cart'),
  flagItem('isStudiesCartEnabled', 'studies_cart_enabled'),
  flagItem('isNetworksCartEnabled', 'networks_cart_enabled'),
  flagItem('isSetsSearchEnabled', 'sets_search_enabled'),
  valueItem('maxNumberOfSets', 'max_number_sets'),
  valueItem('maxItemsPerSet', 'max_items_per_set'),
  valueItem('cartTimeToLive', 'cart_time_to_live'),
  valueItem('setTimeToLive', 'set_time_to_live'),
  groupingItem.value,
]);
</script>
