'use strict';

micaApp
  .config(['$routeProvider', '$httpProvider', '$translateProvider',
    function ($routeProvider, $httpProvider, $translateProvider) {
      $routeProvider
        .when('/config', {
          templateUrl: 'views/config/view.html',
          controller: 'MicaConfigController',
          resolve: {
            resolvedMicaConfig: ['MicaConfig', function (MicaConfig) {
              return MicaConfig.get();
            }]
          }
        })
        .when('/config/edit', {
          templateUrl: 'views/config/form.html',
          controller: 'MicaConfigEditController',
          resolve: {
            resolvedMicaConfig: ['MicaConfig', function (MicaConfig) {
              return MicaConfig.get();
            }]
          }
        })
    }]);
