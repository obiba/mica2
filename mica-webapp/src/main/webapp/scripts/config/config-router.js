'use strict';

mica.config
  .config(['$routeProvider',
    function ($routeProvider) {
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
