/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
