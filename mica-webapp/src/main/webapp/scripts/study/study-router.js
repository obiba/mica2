'use strict';

mica.study
  .config(['$routeProvider', '$httpProvider', '$translateProvider',
    function ($routeProvider, $httpProvider, $translateProvider) {
      $routeProvider
        .when('/study', {
          templateUrl: 'scripts/study/views/list.html',
          controller: 'StudyListController'
        })
        .when('/study/:id', {
          templateUrl: 'scripts/study/views/view.html',
          controller: 'StudyViewController'
        })
        .when('/study/:id/edit', {
          templateUrl: 'scripts/study/views/form.html',
          controller: 'StudyEditController'
        })
    }]);
