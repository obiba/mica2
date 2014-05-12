'use strict';

mica.study
  .config(['$routeProvider', '$httpProvider', '$translateProvider',
    function ($routeProvider, $httpProvider, $translateProvider) {
      $routeProvider
        .when('/study', {
          templateUrl: 'scripts/study/views/study-list.html',
          controller: 'StudyListController'
        })
        .when('/study/:id', {
          templateUrl: 'scripts/study/views/study-view.html',
          controller: 'StudyViewController'
        })
        .when('/study/:id/edit', {
          templateUrl: 'scripts/study/views/study-form.html',
          controller: 'StudyEditController'
        })
    }]);
