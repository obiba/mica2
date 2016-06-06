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

/* global PROJECT_SCHEMA */
/* global PROJECT_DEFINITION */

mica.project

  .constant('PROJECT_EVENTS', {
    projectUpdated: 'event:project-updated'
  })

  .controller('ProjectListController', ['$rootScope',
    '$scope',
    '$filter',
    '$translate',
    'DraftProjectsResource',
    'DraftProjectResource',

    function ($rootScope,
              $scope,
              $filter,
              $translate,
              DraftProjectsResource,
              DraftProjectResource
    ) {
      var onSuccess = function(response) {
        $scope.projects = response;
        $scope.loading = false;
      };

      var onError = function() {
        $scope.loading = false;
      };

      $scope.loading = true;
      DraftProjectsResource.query({}, onSuccess, onError);

      $scope.deleteProject = function(project) {
        DraftProjectResource.delete(project, function() {
          $scope.loading = true;
          DraftProjectsResource.query({}, onSuccess, onError);
        });
      };
    }])


  .controller('ProjectViewController', ['$rootScope',
    '$scope',
    '$routeParams',
    '$log',
    '$locale',
    '$location',
    '$translate',
    'DraftProjectResource',
    'DraftProjectsResource',
    'MicaConfigResource',
    'ProjectFormResource',
    'JsonUtils',
    'PROJECT_EVENTS',
    'NOTIFICATION_EVENTS',
    'DraftFileSystemSearchResource',
    'DraftProjectPermissionsResource',
    'DraftProjectPublicationResource',
    'DraftProjectStatusResource',
    'DraftProjectViewRevisionResource',
    'DraftProjectRevisionsResource',
    'DraftProjectRestoreRevisionResource',
    '$uibModal',
    'LocalizedValues',
    '$filter',
    'DocumentPermissionsService',

    function ($rootScope,
      $scope,
      $routeParams,
      $log,
      $locale,
      $location,
      $translate,
      DraftProjectResource,
      DraftProjectsResource,
      MicaConfigResource,
      ProjectFormResource,
      JsonUtils,
      PROJECT_EVENTS,
      NOTIFICATION_EVENTS,
      DraftFileSystemSearchResource,
      DraftProjectPermissionsResource,
      DraftProjectPublicationResource,
      DraftProjectStatusResource,
      DraftProjectViewRevisionResource,
      DraftProjectRevisionsResource,
      DraftProjectRestoreRevisionResource,
      $uibModal,
      LocalizedValues,
      $filter,
      DocumentPermissionsService) {

      var initializeProject = function(project){
        $scope.activeTab = 0;
        $scope.permissions = DocumentPermissionsService.state(project['obiba.mica.EntityStateDto.projectState']);
        $scope.form.model = JsonUtils.parseJsonSafely(project.content, {});
        // project name/description is an array of map entries
        $scope.form.model._mica = {
           name: LocalizedValues.arrayToObject(project.name),
           description: LocalizedValues.arrayToObject(project.description)
        };
      };

      $scope.Mode = {View: 0, Revision: 1, File: 2, Permission: 3, Comment: 4};

      $scope.activeTab = 0;

      $scope.form = {};

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({lang: lang});
        });
        $scope.languages = micaConfig.languages;
        $scope.sfOptions = {formDefaults: { locales: micaConfig.languages}};
        $scope.roles = micaConfig.roles;
        $scope.openAccess = micaConfig.openAccess;
      });

      ProjectFormResource.get(
        function onSuccess(projectForm) {
          $scope.form.definition = JsonUtils.parseJsonSafely(projectForm.definition, []);
          $scope.form.definition.unshift(angular.copy(PROJECT_DEFINITION));
          $scope.form.schema = JsonUtils.parseJsonSafely(projectForm.schema, {});
          $scope.form.schema.properties._mica = angular.copy(PROJECT_SCHEMA);
          if (!$scope.form.schema.required) {
            $scope.form.schema.required = [];
          }
          $scope.form.schema.required.unshift('_mica.name');
          $scope.form.schema.readonly = true;
        });

      $scope.projectId = $routeParams.id;
      $scope.project = DraftProjectResource.get({id: $routeParams.id}, initializeProject);

      $scope.toStatus = function (value) {
        DraftProjectStatusResource.toStatus({id: $scope.project.id, value: value}, function () {
          $scope.project = DraftProjectResource.get({id: $routeParams.id}, initializeProject);
        });
      };

      $scope.delete = function () {
        DraftProjectResource.delete($scope.project, function() {
          $location.path('/project');
        });
      };

      var getViewMode = function() {
        var result = /\/(revision[s\/]*|files|permissions|comments)/.exec($location.path());

        if (result && result.length > 1) {
          switch (result[1]) {
            case 'revision':
            case 'revisions':
              return $scope.Mode.Revision;
            case 'files':
              return $scope.Mode.File;
            case 'permissions':
              return $scope.Mode.Permission;
            case 'comments':
              return $scope.Mode.Comment;
          }
        }

        return $scope.Mode.View;
      };

      $scope.inViewMode = function () {
        return $scope.viewMode === $scope.Mode.View;
      };

      var viewRevision = function (projectId, commitInfo) {
        $scope.commitInfo = commitInfo;
        $scope.project = DraftProjectViewRevisionResource.view({
          id: projectId,
          commitId: commitInfo.commitId
        }, initializeProject);
      };

      var fetchProject = function (projectId) {
        $scope.project = DraftProjectResource.get({id: projectId}, initializeProject);
      };

      var fetchRevisions = function (projectId, onSuccess) {
        DraftProjectRevisionsResource.query({id: projectId}, function (response) {
          if (onSuccess) {
            onSuccess(response);
          }
        });
      };

      var restoreRevision = function (projectId, commitInfo, onSuccess) {
        if (commitInfo && $scope.projectId === projectId) {
          var args = {commitId: commitInfo.commitId, restoreSuccessCallback: onSuccess};

          $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
            {
              titleKey: 'research-project.restore-dialog.title',
              messageKey: 'research-project.restore-dialog.message',
              messageArgs: [$filter('amDateFormat')(commitInfo.date, 'lll')]
            }, args
          );
        }
      };

      var publish = function (publish) {
        if (publish) {
          DraftProjectPublicationResource.publish(
            {id: $scope.project.id, cascading: 'NONE'},
            function () {
              $scope.project = DraftProjectResource.get({id: $routeParams.id}, initializeProject);
            });
        } else {
          DraftProjectPublicationResource.unPublish({id: $scope.project.id}, function () {
            $scope.project = DraftProjectResource.get({id: $routeParams.id}, initializeProject);
          });
        }
      };

      var onRestore = function (event, args) {
        if (args.commitId) {
          DraftProjectRestoreRevisionResource.restore({id: $scope.projectId, commitId: args.commitId},
            function () {
              fetchProject($routeParams.id);
              $scope.projectId = $routeParams.id;
              if (args.restoreSuccessCallback) {
                args.restoreSuccessCallback();
              }
            });
        }
      };

      var onError = function (response) {
        $rootScope.$broadcast(NOTIFICATION_EVENTS.showNotificationDialog, {
          message: response.data ? response.data : angular.fromJson(response)
        });
      };

      $scope.fetchProject = fetchProject;
      $scope.viewRevision = viewRevision;
      $scope.restoreRevision = restoreRevision;
      $scope.fetchRevisions = fetchRevisions;
      $scope.publish = publish;

      $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, onRestore);

      $scope.emitProjectUpdated = function () {
        $scope.$emit(PROJECT_EVENTS.projectUpdated, $scope.project);
      };

      $scope.$on(PROJECT_EVENTS.projectUpdated, function (event, projectUpdated) {
        if (projectUpdated === $scope.project) {
          $log.debug('save project', projectUpdated);

          $scope.project.$save(function () {
            $scope.project = DraftProjectResource.get({id: $routeParams.id}, initializeProject);
          }, onError);
        }
      });

      $scope.viewMode = getViewMode();
    }])

  .controller('ProjectEditController', [
    '$rootScope',
    '$scope',
    '$routeParams',
    '$log',
    '$locale',
    '$location',
    'DraftProjectResource',
    'DraftProjectsResource',
    'DraftProjectPublicationResource',
    'MicaConfigResource',
    'ProjectFormResource',
    'LocalizedValues',
    'JsonUtils',
    'FormServerValidation',
    'FormDirtyStateObserver',
    function ($rootScope,
              $scope,
              $routeParams,
              $log,
              $locale,
              $location,
              DraftProjectResource,
              DraftProjectsResource,
              DraftProjectPublicationResource,
              MicaConfigResource,
              ProjectFormResource,
              LocalizedValues,
              JsonUtils,
              FormServerValidation,
              FormDirtyStateObserver) {

      $scope.activeTab = 0;
      $scope.files = [];
      $scope.newProject= !$routeParams.id;
      $scope.project = $routeParams.id ?
        DraftProjectResource.get({id: $routeParams.id}, function(response) {
          $scope.files = response.logo ? [response.logo] : [];
          $scope.form.model = JsonUtils.parseJsonSafely(response.content, {});
          // project name/description is an array of map entries
          $scope.form.model._mica = {
            name: LocalizedValues.arrayToObject(response.name),
            description: LocalizedValues.arrayToObject(response.description)
          };
          return response;
        }) : {published: false};

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({lang: lang});
        });
        $scope.languages = micaConfig.languages;
        $scope.sfOptions = {formDefaults: { locales: micaConfig.languages}};
      });

      ProjectFormResource.get(
        function onSuccess(projectForm) {
          $scope.form.definition = JsonUtils.parseJsonSafely(projectForm.definition, []);
          $scope.form.definition.unshift(angular.copy(PROJECT_DEFINITION));
          $scope.form.schema = JsonUtils.parseJsonSafely(projectForm.schema, {});
          $scope.form.schema.properties._mica = angular.copy(PROJECT_SCHEMA);
          if (!$scope.form.schema.required) {
            $scope.form.schema.required = [];
          }
          $scope.form.schema.required.unshift('_mica.name');
          if (!$routeParams.id) {
            $scope.form.model = {};
          }
        });

      $scope.save = function () {
        $scope.$broadcast('schemaFormValidate');
        if ($scope.form.$valid) {
          $scope.project.name = LocalizedValues.objectToArray($scope.languages, $scope.form.model._mica.name);
          $scope.project.description = LocalizedValues.objectToArray($scope.languages, $scope.form.model._mica.description);
          delete $scope.form.model._mica;
          $scope.project.content = JSON.stringify($scope.form.model);
          if ($scope.project.id) {
            updateProject();
          } else {
            createProject();
          }
        } else {
          $scope.form.saveAttempted = true;
        }
      };

      $scope.cancel = function () {
        $location.path('/project' + ($scope.project.id ? '/' + $scope.project.id : '')).replace();
      };

      var updateProject = function () {
        $scope.project.$save(
          function (project) {
            FormDirtyStateObserver.unobserve();
            $location.path('/project/' + project.id).replace();
          },
          saveErrorHandler);
      };

      var createProject = function () {
        DraftProjectsResource.save($scope.project,
          function (resource, getResponseHeaders) {
            var parts = getResponseHeaders().location.split('/');
            FormDirtyStateObserver.unobserve();
            $location.path('/project/' + parts[parts.length - 1]).replace();
          },
          saveErrorHandler);
      };

      var saveErrorHandler = function (response) {
        FormServerValidation.error(response, $scope.form, $scope.languages);
      };

      FormDirtyStateObserver.observe($scope);
    }])


  .controller('ProjectPermissionsController', ['$scope','$routeParams', 'DraftProjectPermissionsResource', 'DraftProjectAccessesResource',
    function ($scope, $routeParams, DraftProjectPermissionsResource, DraftProjectAccessesResource) {
      $scope.permissions = [];
      $scope.accesses = [];

      $scope.loadPermissions = function () {
        $scope.permissions = DraftProjectPermissionsResource.query({id: $routeParams.id});
        return $scope.permissions;
      };

      $scope.deletePermission = function (permission) {
        return DraftProjectPermissionsResource.delete({id: $routeParams.id}, permission);
      };

      $scope.addPermission = function (permission) {
        return DraftProjectPermissionsResource.save({id: $routeParams.id}, permission);
      };

      $scope.loadAccesses = function () {
        $scope.accesses = DraftProjectAccessesResource.query({id: $routeParams.id});
        return $scope.accesses;
      };

      $scope.deleteAccess = function (access) {
        return DraftProjectAccessesResource.delete({id: $routeParams.id}, access);
      };

      $scope.addAccess = function (access) {
        return DraftProjectAccessesResource.save({id: $routeParams.id}, access);
      };
    }]);
