'use strict';

mica.study
  .config(['$routeProvider',
    function ($routeProvider) {
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
