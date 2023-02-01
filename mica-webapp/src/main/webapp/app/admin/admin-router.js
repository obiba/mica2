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

mica.admin

  .config(['$routeProvider',
    function ($routeProvider) {
      $routeProvider
        .when('/admin', {
          templateUrl: 'app/admin/views/admin-view.html',
          controller: 'AdminViewController',
          access: {
            authorizedRoles: ['mica-administrator']
          }
        })
        .when('/admin/metrics', {
          templateUrl: 'app/admin/views/metrics.html',
          controller: 'MetricsController',
          access: {
            authorizedRoles: ['mica-administrator']
          }
        })
        .when('/admin/caching', {
          templateUrl: 'app/admin/views/caching.html',
          controller: 'CachingController',
          access: {
            authorizedRoles: ['mica-administrator']
          }
        })
        .when('/admin/classifications', {
          templateUrl: 'app/admin/views/classifications.html',
          controller: 'ClassificationsController',
          access: {
            authorizedRoles: ['mica-administrator']
          }
        })
        .when('/admin/indexing', {
          templateUrl: 'app/admin/views/indexing.html',
          controller: 'IndexingController',
          access: {
            authorizedRoles: ['mica-administrator']
          }
        })
        .when('/admin/logs', {
          templateUrl: 'app/admin/views/logs.html',
          controller: 'LogsController',
          access: {
            authorizedRoles: ['mica-administrator']
          }
        })
        .when('/admin/files', {
          templateUrl: 'app/admin/views/file-system.html',
          reloadOnSearch: false,
          access: {
            authorizedRoles: ['mica-administrator']
          }
        })
        .when('/admin/statistics-summary', {
          templateUrl: 'app/admin/views/entity-statistics-summary.html',
          access: {
            authorizedRoles: ['mica-administrator']
          }
        });
    }]);
