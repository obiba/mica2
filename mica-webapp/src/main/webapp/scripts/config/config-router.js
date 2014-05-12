'use strict';

mica.config
  .config(['$routeProvider', '$httpProvider', '$translateProvider',
    function ($routeProvider, $httpProvider, $translateProvider) {
      $routeProvider
        .when('/config', {
          templateUrl: 'views/config/view.html',
          controller: 'MicaConfigController'
        })
        .when('/config/edit', {
          templateUrl: 'views/config/form.html',
          controller: 'MicaConfigEditController'
        })
    }]);
