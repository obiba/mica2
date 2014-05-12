'use strict';

mica.config
  .config(['$routeProvider', '$httpProvider', '$translateProvider',
    function ($routeProvider, $httpProvider, $translateProvider) {
      $routeProvider
        .when('/config', {
          templateUrl: 'scripts/config/config-view.html',
          controller: 'MicaConfigController'
        })
        .when('/config/edit', {
          templateUrl: 'scripts/config/config-form.html',
          controller: 'MicaConfigEditController'
        })
    }]);
