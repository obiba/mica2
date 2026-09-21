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

const routes: RouteRecordRaw[] = [
  { path: '/index.html', redirect: '/' },
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', component: () => import('pages/IndexPage.vue') },
      { path: 'settings', component: () => import('pages/SettingsPage.vue') },
      { path: 'settings/general', component: () => import('pages/SettingsGeneralPage.vue') },
      ...DOCUMENT_TYPES.map((documentType) => ({
        path: entityConfigRoute(documentType).substring(1),
        component: () => import('pages/SettingsEntityConfigPage.vue'),
        meta: { documentType },
      })),
      ...DOCUMENT_TYPES.flatMap(documentRoutes),
      { path: 'files', component: () => import('pages/FilesPage.vue') },
      { path: 'persons', component: () => import('pages/PersonsPage.vue') },
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
