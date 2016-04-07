'use strict';

mica.config
  .config(['$routeProvider',
    function ($routeProvider) {
      $routeProvider
        .when('/admin/general', {
          templateUrl: 'app/config/views/config-view.html',
          controller: 'MicaConfigController'
        })
        .when('/admin/general/edit', {
          templateUrl: 'app/config/views/config-form.html',
          controller: 'MicaConfigEditController'
        })
        .when('/admin/style/edit', {
          templateUrl: 'app/config/views/config-style-form.html',
          controller: 'MicaConfigStyleEditController'
        });
    }]);
