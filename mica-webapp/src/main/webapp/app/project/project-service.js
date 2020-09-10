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

mica.project

  .factory('DraftProjectsResource', ['$resource', 'ProjectModelFactory',
    function ($resource, ProjectModelFactory) {
      return $resource(contextPath + '/ws/draft/projects?comment:comment', {}, {
        'get' : {method: 'GET', errorHandler: true},
        'save': {method: 'POST', errorHandler: true, transformRequest: ProjectModelFactory.serialize}
      });
    }])

  .factory('DraftProjectResource', ['$resource', 'ProjectModelFactory',
    function ($resource, ProjectModelFactory) {
      return $resource(contextPath + '/ws/draft/project/:id', {}, {
        'save': {method: 'PUT', params: {id: '@id'}, errorHandler: true, transformRequest: ProjectModelFactory.serialize},
        'rSave': {method: 'PUT', params: {id: '@id'}, errorHandler: true, transformRequest: ProjectModelFactory.serializeForRestoringFields},
        'delete': {method: 'DELETE', params: {id: '@id'}, errorHandler: true},
        'get': {method: 'GET', errorHandler: true, transformResponse: ProjectModelFactory.deserialize},
        'rGet': {method: 'GET', errorHandler: true, transformResponse: ProjectModelFactory.deserializeForRestoringFields}
      });
    }])

  .factory('DraftProjectPublicationResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/draft/project/:id/_publish', {id: '@id'}, {
        'publish': {method: 'PUT', params: {cascading: '@cascading'}},
        'unPublish': {method: 'DELETE'}
      });
    }])

  .factory('DraftProjectStatusResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/draft/project/:id/_status', {}, {
        'toStatus': {method: 'PUT', params: {id: '@id', value: '@value'}}
      });
    }])

  .factory('DraftProjectRevisionsResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/draft/project/:id/commits', {}, {
        'get': {method: 'GET', params: {id: '@id'}},
        'diff': {method: 'GET', url: contextPath + '/ws/draft/project/:id/_diff', params: {id: '@id'}}
      });
    }])

  .factory('DraftProjectRestoreRevisionResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/draft/project/:id/commit/:commitId/restore', {}, {
        'restore': {method: 'PUT', params: {id: '@id', commitId: '@commitId'}}
      });
    }])

  .factory('DraftProjectViewRevisionResource', ['$resource', 'ProjectModelFactory',
    function ($resource, ProjectModelFactory) {
      return $resource(contextPath + '/ws/draft/project/:id/commit/:commitId/view', {}, {
        'view': {method: 'GET', params: {id: '@id', commitId: '@commitId'}, transformResponse: ProjectModelFactory.deserialize}
      });
    }])

  .factory('DraftProjectPermissionsResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/draft/project/:id/permissions', {}, {
        'save': {
          method: 'PUT',
          params: {id: '@id', type: '@type', principal: '@principal', role: '@role', file: '@file'},
          errorHandler: true
        },
        'delete': {method: 'DELETE', params: {id: '@id', type: '@type', principal: '@principal'}, errorHandler: true},
        'get': {method: 'GET'},
        'query': {method: 'GET', params: {id: '@id'}, isArray: true}
      });
    }])

  .factory('DraftProjectAccessesResource', ['$resource',
    function ($resource) {
      return $resource(contextPath + '/ws/draft/project/:id/accesses', {}, {
        'save': {
          method: 'PUT',
          params: {id: '@id', type: '@type', principal: '@principal', file: '@file'},
          errorHandler: true
        },
        'delete': {method: 'DELETE', params: {id: '@id', type: '@type', principal: '@principal'}, errorHandler: true},
        'get': {method: 'GET'},
        'query': {method: 'GET', params: {id: '@id'}, isArray: true}
      });
    }])

  .factory('ProjectModelFactory', ['LocalizedValues',
    function (LocalizedValues) {
      this.serialize = function (project) {
        return serialize(project, true);
      };

      this.deserialize = function (data) {
        return deserialize(data, true);
      };

      this.serializeForRestoringFields = function (project) {
        return serialize(project, false);
      };

      this.deserializeForRestoringFields = function (data) {
        return deserialize(data, false);
      };

      function serialize(project, normal) {
        var projectCopy = angular.copy(project);

        if (normal) {
          projectCopy.title = LocalizedValues.objectToArray(projectCopy.model._title);
          projectCopy.summary = LocalizedValues.objectToArray(projectCopy.model._summary);
          delete projectCopy.model._title;
          delete projectCopy.model._summary;
        } else {
          projectCopy.title = LocalizedValues.objectToArray(projectCopy.title);
          projectCopy.summary = LocalizedValues.objectToArray(projectCopy.summary);
        }

        projectCopy.content = projectCopy.model ? angular.toJson(projectCopy.model) : null;
        delete projectCopy.model;
        return angular.toJson(projectCopy);
      }

      function deserialize(data, normal) {
        if (!data) {
          return {model: {}};
        }

        var project = angular.fromJson(data);
        project.model = project.content ? angular.fromJson(project.content) : {};

        if (normal) {
          project.model._title = LocalizedValues.arrayToObject(project.title);
          project.model._summary = LocalizedValues.arrayToObject(project.summary);
        } else {
          project.title = LocalizedValues.arrayToObject(project.title);
          project.summary = LocalizedValues.arrayToObject(project.summary);
        }

        return project;
      }

      return this;
    }]);
