'use strict';

micaApp
  .config(['$routeProvider', '$httpProvider', '$translateProvider',
    function ($routeProvider, $httpProvider, $translateProvider) {
      $routeProvider
        .when('/micaConfig', {
          templateUrl: 'views/micaConfig/view.html',
          controller: 'MicaConfigController',
          resolve: {
            resolvedMicaConfig: ['MicaConfig', function (MicaConfig) {
              return MicaConfig.get();
            }]
          }
        })
    }]);
