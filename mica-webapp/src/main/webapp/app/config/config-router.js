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
        .when('/admin/notifications', {
          templateUrl: 'app/config/views/config-notifications-view.html',
          controller: 'MicaConfigController'
        })
        .when('/admin/notifications/edit', {
          templateUrl: 'app/config/views/config-notifications-form.html',
          controller: 'MicaConfigNotificationsEditController'
        })
        .when('/admin/translations/edit', {
          templateUrl: 'app/config/views/config-translations-form.html',
          controller: 'MicaConfigTranslationsEditController'
        }).when('/admin/style/edit', {
          templateUrl: 'app/config/views/config-style-form.html',
          controller: 'MicaConfigStyleEditController'
        });
    }]);
