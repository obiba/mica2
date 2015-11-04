'use strict';

mica.study
  .config(['$routeProvider',
    function ($routeProvider) {
      $routeProvider
        .when('/study', {
          templateUrl: 'app/study/views/study-list.html',
          controller: 'StudyListController'
        })
        .when('/study/new', {
          templateUrl: 'app/study/views/study-form.html',
          controller: 'StudyEditController'
        })
        .when('/study/:id', {
          templateUrl: 'app/study/views/study-view.html',
          controller: 'StudyViewController'
        })
        .when('/study/:id/edit', {
          templateUrl: 'app/study/views/study-form.html',
          controller: 'StudyEditController'
        })
        .when('/study/:id/revisions', {
          templateUrl: 'app/study/views/study-view-revisions.html',
          controller: 'StudyViewController'
        })
        .when('/study/:id/files', {
          templateUrl: 'app/study/views/study-view-files.html',
          controller: 'StudyViewController',
          reloadOnSearch: false
        })
        .when('/study/:id/permissions', {
          templateUrl: 'app/study/views/study-view-permissions.html',
          controller: 'StudyViewController'
        })
        .when('/study/:id/population/add', {
          templateUrl: 'app/study/views/population/population-form.html',
          controller: 'StudyPopulationController'
        })
        .when('/study/:id/population/:pid/edit', {
          templateUrl: 'app/study/views/population/population-form.html',
          controller: 'StudyPopulationController'
        })
        .when('/study/:id/population/:pid/dce/add', {
          templateUrl: 'app/study/views/population/dce/data-collection-event-form.html',
          controller: 'StudyPopulationDceController'
        })
        .when('/study/:id/population/:pid/dce/:dceId/edit', {
          templateUrl: 'app/study/views/population/dce/data-collection-event-form.html',
          controller: 'StudyPopulationDceController'
        });
    }]);
