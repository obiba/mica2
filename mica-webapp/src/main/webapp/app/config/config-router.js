'use strict';

mica.config
  .config(['$routeProvider',
    function ($routeProvider) {
      $routeProvider
        .when('/config', {
          templateUrl: 'app/config/views/config-view.html',
          controller: 'MicaConfigController'
        })
        .when('/config/edit', {
          templateUrl: 'app/config/views/config-form.html',
          controller: 'MicaConfigEditController'
        });
    }]);
