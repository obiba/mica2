'use strict';

micaApp
  .config(['$routeProvider', '$httpProvider', '$translateProvider',
    function ($routeProvider, $httpProvider, $translateProvider) {
      $routeProvider
        .when('/study', {
          templateUrl: 'views/studys.html',
          controller: 'StudyController',
          resolve: {
            resolvedStudy: ['Study', function (Study) {
              return Study.query();
            }]
          }
        })
    }]);
