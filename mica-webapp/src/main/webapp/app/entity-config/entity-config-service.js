/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

mica.entityConfig

  .factory('EntityFormResource', ['$resource',
    function ($resource) {
      return $resource('ws/config/:target/form', {}, {
        'get': {method: 'GET', errorHandler: true},
        'save': {method: 'PUT', errorHandler: true}
      });
    }])

  .factory('EntityFormPermissionsResource', ['$resource',
    function ($resource) {
      return $resource('ws/config/:target/permissions', {}, {
        'save': {
          method: 'PUT',
          params: {target: '@target', type: '@type', principal: '@principal', role: '@role'},
          errorHandler: true
        },
        'delete': {method: 'DELETE', params: {target: '@target', type: '@type', principal: '@principal'}, errorHandler: true},
        'get': {method: 'GET', params: {target: '@target'}, isArray: true}
      });
    }])

  .factory('EntityFormAccessesResource', ['$resource',
    function ($resource) {
      return $resource('ws/config/:target/accesses', {}, {
        'save': {
          method: 'PUT',
          params: {target: '@target', type: '@type', principal: '@principal', role: '@role'},
          errorHandler: true
        },
        'delete': {method: 'DELETE', params: {target: '@target', type: '@type', principal: '@principal'}, errorHandler: true},
        'get': {method: 'GET', params: {target: '@target'}, isArray: true}
      });
    }]);
