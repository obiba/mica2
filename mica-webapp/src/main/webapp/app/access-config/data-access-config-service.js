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

mica.dataAccessConfig

  .factory('DataAccessFormResource', ['$resource',
    function ($resource) {
      return $resource('ws/config/data-access-form', {}, {
        'get': {method: 'GET', errorHandler: true},
        'save': {method: 'PUT', errorHandler: true}
      });
    }])

  .factory('DataAccessFormPermissionsResource', ['$resource',
    function ($resource) {
      return $resource('ws/config/data-access-form/permissions', {}, {
        'save': {
          method: 'PUT',
          params: {type: '@type', principal: '@principal', role: '@role', otherResources: '@others'},
          errorHandler: true
        },
        'delete': {method: 'DELETE', params: {type: '@type', principal: '@principal'}, errorHandler: true},
        'get': {method: 'GET', isArray: true}
      });
    }])
  .factory('DataAccessAmendmentFormResource', ['$resource',
    function ($resource) {
      return $resource('ws/config/data-access-amendment-form', {}, {
        'get': {method: 'GET', errorHandler: true},
        'save': {method: 'PUT', errorHandler: true}
      });
    }]);
