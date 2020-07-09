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

mica.projectConfig

  .factory('ProjectFormResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/config/project/form', {}, {
        'get': {method: 'GET', params: {locale: '@locale'}, errorHandler: true}
      });
    }])

  .factory('ProjectFormCustomResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/config/project/form-custom', {}, {
        'get': {method: 'GET', errorHandler: true},
        'save': {method: 'PUT', errorHandler: true}
      });
    }])

  .factory('ProjectFormPermissionsResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/config/project/permissions', {}, {
        'save': {
          method: 'PUT',
          params: {draft: '@draft', type: '@type', principal: '@principal', role: '@role', file: '@file'},
          errorHandler: true
        },
        'delete': {method: 'DELETE', params: {draft: '@draft', type: '@type', principal: '@principal'}, errorHandler: true},
        'get': {method: 'GET', params: {draft: '@draft'}, isArray: true}
      });
    }])

  .factory('ProjectFormAccessesResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/config/project/accesses', {}, {
        'save': {
          method: 'PUT',
          params: {draft: '@draft', type: '@type', principal: '@principal', role: '@role', file: '@file'},
          errorHandler: true
        },
        'delete': {method: 'DELETE', params: {draft: '@draft', type: '@type', principal: '@principal'}, errorHandler: true},
        'get': {method: 'GET', params: {draft: '@draft'}, isArray: true}
      });
    }]);
