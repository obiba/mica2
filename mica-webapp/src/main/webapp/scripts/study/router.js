'use strict';

micaApp
  .config(['$routeProvider', '$httpProvider', '$translateProvider',
    function ($routeProvider, $httpProvider, $translateProvider) {
      $routeProvider
        .when('/study', {
          templateUrl: 'views/study/studies.html',
          controller: 'StudyListController'
        })
        .when('/study/:id', {
          templateUrl: 'views/study/study-view.html',
          controller: 'StudyViewController'
        })
        .when('/study/:id/edit', {
          templateUrl: 'views/study/study-form.html',
          controller: 'StudyEditController'
        })
    }]);
