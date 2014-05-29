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
        });
    }]);
