import type { RouteRecordRaw } from 'vue-router';
import { DOCUMENT_TYPES, documentTarget, type DocumentType } from 'src/composables/useDocumentTarget';
import { entityConfigRoute } from 'src/utils/entityConfigs';

/** the list, creation, edition and view routes of a document type, served by the shared pages */
function documentRoutes(documentType: DocumentType): RouteRecordRaw[] {
  const meta = { documentType };
  const { listRoute } = documentTarget(documentType, '');
  return [
    { path: listRoute.substring(1), component: () => import('pages/DocumentsPage.vue'), meta },
    { path: `${documentType}/new`, component: () => import('pages/DocumentEditPage.vue'), meta },
    { path: `${documentType}/:id/edit`, component: () => import('pages/DocumentEditPage.vue'), meta },
    { path: `${documentType}/:id/:tab?`, component: () => import('pages/DocumentPage.vue'), meta },
  ];
}

/** the editors of the populations of an individual study and of their data collection events */
function studyPopulationRoutes(): RouteRecordRaw[] {
  const component = () => import('pages/StudyPopulationEditPage.vue');
  const population = { documentType: 'individual-study' as const, studyPart: 'population' as const };
  const dce = { ...population, studyPart: 'dce' as const };
  const base = 'individual-study/:id/population';
  return [
    { path: `${base}/new`, component, meta: population },
    { path: `${base}/:pid/edit`, component, meta: population },
    { path: `${base}/:pid/dce/new`, component, meta: dce },
    { path: `${base}/:pid/dce/:dceId/edit`, component, meta: dce },
  ];
}

declare module 'vue-router' {
  interface RouteMeta {
    /** the part of the study a study editor route edits */
    studyPart?: 'population' | 'dce';
  }
}

const routes: RouteRecordRaw[] = [
  { path: '/index.html', redirect: '/' },
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', component: () => import('pages/IndexPage.vue') },
      { path: 'settings', component: () => import('pages/SettingsPage.vue') },
      { path: 'settings/general', component: () => import('pages/SettingsGeneralPage.vue') },
      { path: 'settings/notifications', component: () => import('pages/SettingsNotificationsPage.vue') },
      { path: 'settings/data-access', component: () => import('pages/SettingsDataAccessPage.vue') },
      { path: 'settings/caching', component: () => import('pages/SettingsCachingPage.vue') },
      { path: 'settings/logs', component: () => import('pages/SettingsLogsPage.vue') },
      { path: 'settings/translations', component: () => import('pages/SettingsTranslationsPage.vue') },
      { path: 'settings/indexing', component: () => import('pages/SettingsIndexingPage.vue') },
      { path: 'settings/statistics', component: () => import('pages/SettingsStatisticsPage.vue') },
      { path: 'settings/taxonomies/:tab?', component: () => import('pages/SettingsTaxonomiesPage.vue') },
      ...DOCUMENT_TYPES.map((documentType) => ({
        path: entityConfigRoute(documentType).substring(1),
        component: () => import('pages/SettingsEntityConfigPage.vue'),
        meta: { documentType },
      })),
      ...studyPopulationRoutes(),
      ...DOCUMENT_TYPES.flatMap(documentRoutes),
      { path: 'files', component: () => import('pages/FilesPage.vue') },
      { path: 'persons', component: () => import('pages/PersonsPage.vue') },
      { path: 'persons/:id/:tab?', component: () => import('pages/PersonPage.vue') },
    ],
  },

  // Always leave this as last one,
  // but you can also remove it
  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorNotFound.vue'),
  },
];

export default routes;
