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

      $scope.deleteNetwork = function(project) {
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
    'PROJECT_EVENTS',
    'NOTIFICATION_EVENTS',
    'DraftFileSystemSearchResource',
    'DraftProjectPermissionsResource',
    'DraftProjectPublicationResource',
    'DraftProjectStatusResource',
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
      PROJECT_EVENTS,
      NOTIFICATION_EVENTS,
      DraftFileSystemSearchResource,
      DraftProjectPermissionsResource,
      DraftProjectPublicationResource,
      DraftProjectStatusResource,
      $uibModal,
      LocalizedValues,
      $filter,
      DocumentPermissionsService) {
      
      var initializeProject = function(project){
        $scope.activeTab = 0;
        $scope.permissions = DocumentPermissionsService.state(project['obiba.mica.EntityStateDto.projectState']);
      };

      $scope.Mode = {View: 0, Revision: 1, File: 2, Permission: 3, Comment: 4};

      $scope.activeTab = 0;

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({lang: lang});
        });

        $scope.roles = micaConfig.roles;
        $scope.openAccess = micaConfig.openAccess;
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
        $scope.project = DraftNetworkViewRevisionResource.view({
          id: projectId,
          commitId: commitInfo.commitId
        }, initializeProject);
      };

      var fetchProject = function (projectId) {
        $scope.project = DraftProjectResource.get({id: projectId}, initializeProject);
      };

      var fetchRevisions = function (projectId, onSuccess) {
        DraftNetworkRevisionsResource.query({id: projectId}, function (response) {
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
              titleKey: 'project.restore-dialog.title',
              messageKey: 'project.restore-dialog.message',
              messageArgs: [$filter('amDateFormat')(commitInfo.date, 'lll')]
            }, args
          );
        }
      };

      var publish = function (publish) {
        if (publish) {
          // DraftFileSystemSearchResource.searchUnderReview({path: '/project/' + $scope.project.id},
          //   function onSuccess(response) {
          DraftProjectPublicationResource.publish(
                {id: $scope.project.id, cascading: 'NONE'},
                function () {
                  $scope.project = DraftProjectResource.get({id: $routeParams.id}, initializeProject);
                });
          //   },
          //   function onError() {
          //     $log.error('Failed to search for Under Review files.');
          //   }
          // );
        } else {
          DraftProjectPublicationResource.unPublish({id: $scope.project.id}, function () {
            $scope.project = DraftProjectResource.get({id: $routeParams.id}, initializeProject);
          });
        }
      };

      var onRestore = function (event, args) {
        if (args.commitId) {
          DraftNetworkRestoreRevisionResource.restore({id: $scope.projectId, commitId: args.commitId},
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
    }]);