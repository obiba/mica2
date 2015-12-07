/*
 * Copyright (c) 2014 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

angular.module('obiba.mica.access')
  .config(['$routeProvider',
    function ($routeProvider) {
      $routeProvider
        .when('/data-access-requests', {
          templateUrl: 'access/views/data-access-request-list.html',
          controller: 'DataAccessRequestListController'
        })
        .when('/data-access-request/new', {
          templateUrl: 'access/views/data-access-request-form.html',
          controller: 'DataAccessRequestEditController'
        })
        .when('/data-access-request/:id/edit', {
          templateUrl: 'access/views/data-access-request-form.html',
          controller: 'DataAccessRequestEditController'
        })
        .when('/data-access-request/:id', {
          templateUrl: 'access/views/data-access-request-view.html',
          controller: 'DataAccessRequestViewController'
        });
    }]);
