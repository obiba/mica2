<template>
  <div>
    <div class="row items-start no-wrap q-mb-md">
      <div class="col">
        <q-chip dense square :icon="TAXONOMY_NODE_ICONS[node.type]" class="q-ml-none">
          {{ t(`taxonomies.types.${node.type}`) }}
        </q-chip>
        <div class="text-h6">{{ title }}</div>
        <div v-if="title !== node.name" class="text-caption text-grey-7">{{ node.name }}</div>
      </div>
      <slot name="actions" />
    </div>

    <div v-if="description" class="text-grey-8 q-mb-md">{{ description }}</div>

    <q-list dense class="q-mb-md">
      <q-item v-for="property in properties" :key="property.label">
        <q-item-section class="text-grey-7" style="max-width: 180px">{{ t(property.label) }}</q-item-section>
        <q-item-section>{{ property.value }}</q-item-section>
      </q-item>
    </q-list>

    <template v-if="translations.length">
      <div class="text-subtitle2 q-mb-sm">{{ t('taxonomies.translations') }}</div>
      <q-markup-table flat bordered dense class="q-mb-md">
        <thead>
          <tr>
            <th class="text-left">{{ t('taxonomies.locale') }}</th>
            <th class="text-left">{{ t('taxonomies.title_column') }}</th>
            <th class="text-left">{{ t('description') }}</th>
            <th v-if="hasKeywords" class="text-left">{{ t('taxonomies.keywords') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in translations" :key="row.locale">
            <td><q-badge color="primary">{{ row.locale }}</q-badge></td>
            <td class="wrap">{{ row.title }}</td>
            <td class="wrap">{{ row.description }}</td>
            <td v-if="hasKeywords" class="wrap">{{ row.keywords }}</td>
          </tr>
        </tbody>
      </q-markup-table>
    </template>

    <template v-if="attributes.length">
      <div class="text-subtitle2 q-mb-sm">{{ t('attributes') }}</div>
      <q-markup-table flat bordered dense>
        <thead>
          <tr>
            <th class="text-left">{{ t('name') }}</th>
            <th class="text-left">{{ t('value') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(attribute, idx) in attributes" :key="idx">
            <td class="text-caption">{{ attribute.key }}</td>
            <td class="wrap">{{ attribute.value }}</td>
          </tr>
        </tbody>
      </q-markup-table>
    </template>
  </div>
</template>

<script setup lang="ts">
import { TAXONOMY_NODE_ICONS, type TaxonomyNode } from 'src/composables/useTaxonomies';
import type { LocaleTextDto, TaxonomyDto, VocabularyDto } from 'src/models/Opal';
import { localeText } from 'src/utils/config';

const props = defineProps<{ node: TaxonomyNode }>();

const { t, locale } = useI18n();

const entity = computed(() => props.node.entity);

const title = computed(() =>
  props.node.type === 'target'
    ? t(`taxonomies.targets.${props.node.name}`)
    : localeText(entity.value?.title, locale.value, props.node.name),
);

const description = computed(() => localeText(entity.value?.description, locale.value));

const attributes = computed(() => entity.value?.attributes || []);

const properties = computed(() => {
  const count = props.node.children?.length ?? 0;
  const rows: { label: string; value: string | number }[] = [];
  if (props.node.type === 'taxonomy') {
    const taxonomy = entity.value as TaxonomyDto;
    if (taxonomy.author) rows.push({ label: 'taxonomies.author', value: taxonomy.author });
    if (taxonomy.license) rows.push({ label: 'taxonomies.license', value: taxonomy.license });
    rows.push({ label: 'taxonomies.vocabularies', value: count });
  } else if (props.node.type === 'vocabulary') {
    const repeatable = (entity.value as VocabularyDto).repeatable === true;
    rows.push({ label: 'taxonomies.repeatable', value: t(repeatable ? 'taxonomies.yes' : 'taxonomies.no') });
    rows.push({ label: 'taxonomies.terms', value: count });
  } else if (props.node.type === 'term') {
    if (count) rows.push({ label: 'taxonomies.terms', value: count });
  } else if (props.node.children) {
    rows.push({ label: 'taxonomies.taxonomies', value: count });
  }
  return rows;
});

const hasKeywords = computed(() => (entity.value?.keywords || []).length > 0);

/** one row per locale of the title, description and keywords */
const translations = computed(() => {
  const texts = (list: LocaleTextDto[] | undefined, loc: string) =>
    (list || [])
      .filter((text) => text.locale === loc)
      .map((text) => text.text)
      .join(', ');
  const current = entity.value;
  if (!current) return [];
  const locales = [
    ...new Set(
      [...(current.title || []), ...(current.description || []), ...(current.keywords || [])].map((x) => x.locale),
    ),
  ];
  return locales.map((loc) => ({
    locale: loc,
    title: texts(current.title, loc),
    description: texts(current.description, loc),
    keywords: texts(current.keywords, loc),
  }));
});
</script>

<style scoped>
.wrap {
  white-space: normal;
}
</style>
