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

mica.project

  .factory('DraftProjectsResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/projects', {}, {
        'get' : {method: 'GET', errorHandler: true},
        'save': {method: 'POST', errorHandler: true}
      });
    }])

  .factory('DraftProjectResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/project/:id', {}, {
        'save': {method: 'PUT', params: {id: '@id'}, errorHandler: true},
        'delete': {method: 'DELETE', params: {id: '@id'}, errorHandler: true},
        'get': {method: 'GET', errorHandler: true}
      });
    }])

  .factory('DraftProjectPublicationResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/project/:id/_publish', {id: '@id'}, {
        'publish': {method: 'PUT', params: {cascading: '@cascading'}},
        'unPublish': {method: 'DELETE'}
      });
    }])

  .factory('DraftProjectStatusResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/project/:id/_status', {}, {
        'toStatus': {method: 'PUT', params: {id: '@id', value: '@value'}}
      });
    }])

  .factory('DraftProjectRevisionsResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/project/:id/commits', {}, {
        'get': {method: 'GET', params: {id: '@id'}}
      });
    }])

  .factory('DraftProjectRestoreRevisionResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/project/:id/commit/:commitId/restore', {}, {
        'restore': {method: 'PUT', params: {id: '@id', commitId: '@commitId'}}
      });
    }])

  .factory('DraftProjectViewRevisionResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/project/:id/commit/:commitId/view', {}, {
        'view': {method: 'GET', params: {id: '@id', commitId: '@commitId'}}
      });
    }])

  .factory('DraftProjectPermissionsResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/project/:id/permissions', {}, {
        'save': {
          method: 'PUT',
          params: {id: '@id', type: '@type', principal: '@principal', role: '@role'},
          errorHandler: true
        },
        'delete': {method: 'DELETE', params: {id: '@id', type: '@type', principal: '@principal'}, errorHandler: true},
        'get': {method: 'GET'},
        'query': {method: 'GET', params: {id: '@id'}, isArray: true}
      });
    }])

  .factory('DraftProjectAccessesResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/project/:id/accesses', {}, {
        'save': {
          method: 'PUT',
          params: {id: '@id', type: '@type', principal: '@principal', file: '@file'},
          errorHandler: true
        },
        'delete': {method: 'DELETE', params: {id: '@id', type: '@type', principal: '@principal'}, errorHandler: true},
        'get': {method: 'GET'},
        'query': {method: 'GET', params: {id: '@id'}, isArray: true}
      });
    }]);
