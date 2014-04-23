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
    }]);
