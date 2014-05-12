'use strict';

mica.study
  .config(['$routeProvider', '$httpProvider', '$translateProvider',
    function ($routeProvider, $httpProvider, $translateProvider) {
      $routeProvider
        .when('/study', {
          templateUrl: 'views/study/list.html',
          controller: 'StudyListController'
        })
        .when('/study/:id', {
          templateUrl: 'views/study/view.html',
          controller: 'StudyViewController'
        })
        .when('/study/:id/edit', {
          templateUrl: 'views/study/form.html',
          controller: 'StudyEditController'
        })
    }]);
