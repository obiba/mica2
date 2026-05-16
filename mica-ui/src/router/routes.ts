import type { RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  { path: '/index.html', redirect: '/' },
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', component: () => import('pages/IndexPage.vue') },
      { path: 'settings', component: () => import('pages/SettingsPage.vue') },
      { path: 'networks', component: () => import('pages/NetworksPage.vue') },
      { path: 'network/:id', component: () => import('pages/NetworkPage.vue') },
      { path: 'individual-studies', component: () => import('pages/IndividualStudiesPage.vue') },
      { path: 'harmonization-studies', component: () => import('pages/HarmonizationStudiesPage.vue') },
      { path: 'collected-datasets', component: () => import('pages/IndividualDatasetsPage.vue') },
      { path: 'harmonized-datasets', component: () => import('pages/HarmonizedDatasetsPage.vue') },
      { path: 'files', component: () => import('pages/FilesPage.vue') },
      { path: 'persons', component: () => import('pages/PersonsPage.vue') },
      { path: 'research-projects', component: () => import('pages/ResearchProjectsPage.vue') },
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
