'use strict';

mica.config
  .config(['$routeProvider',
    function ($routeProvider) {
      $routeProvider
        .when('/config', {
          templateUrl: 'app/config/config-view.html',
          controller: 'MicaConfigController'
        })
        .when('/config/edit', {
          templateUrl: 'app/config/config-form.html',
          controller: 'MicaConfigEditController'
        });
    }]);
