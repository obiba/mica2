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

/* global location */

mica.project

  .constant('PROJECT_EVENTS', {
    projectUpdated: 'event:project-updated'
  })

  .controller('ProjectListController', ['$rootScope',
    '$scope',
    '$filter',
    '$translate',
    '$timeout',
    'DraftProjectsResource',
    'DraftProjectResource',
    'AlertBuilder',
    'EntityStateFilterService',
    'MicaConfigResource',

    function ($rootScope,
              $scope,
              $filter,
              $translate,
              $timeout,
              DraftProjectsResource,
              DraftProjectResource,
              AlertBuilder,
              EntityStateFilterService,
              MicaConfigResource) {

     function onFilterSelected(filter) {
        EntityStateFilterService.updateUrl(filter);
        $scope.filter = filter;
        $scope.pagination.current = 1;
        loadPage($scope.pagination.current);
      }


      function onSearchFieldSelected(field) {
        console.debug(`Search field: ${field}`);
        let index = $scope.search.fields.indexOf(field);
        if (index > -1) {
          $scope.search.queryField = $scope.search.fields[index];
        } else {
          console.error('Invalid Field');
        }

        if ($scope.pagination.searchText) {
          loadPage($scope.pagination.current);
        }
      }

      var onSuccess = function(response) {
        $scope.projects = response.projects;
        $scope.totalCount = parseInt(response.total, 10);
        $scope.loading = false;

        if (!$scope.hasProjects) {
          $scope.hasProjects = $scope.totalCount && !$scope.pagination.searchText;
        }

        $scope.canAdd = response.actions && response.actions.indexOf('ADD') > -1;
      };

      var onError = function() {
        $scope.loading = false;
      };

      $scope.onFilterSelected = onFilterSelected;
      $scope.onSearchFieldSelected = onSearchFieldSelected;

      $scope.deleteProject = function(project) {
        DraftProjectResource.delete(project, function() {
          refreshPage();
        });
      };

      $scope.pageChanged = function(page) {
        loadPage(page, $scope.pagination.searchText);
      };

      function loadPage(page) {
        let data = {
          from:(page - 1) * $scope.limit,
          limit: $scope.limit,
          filter: $scope.filter,
          sort: $scope.sort.column,
          order: $scope.sort.order
        };

        if($scope.pagination.searchText) {
          data.query = mica.commons.addQueryFields(mica.commons.cleanupQuery($scope.pagination.searchText), $scope.search.queryField, getValidLocale());
        }

        DraftProjectsResource.get(data, onSuccess, AlertBuilder.newBuilder().onError(onError));
      }

      var currentSearch = null;

      function refreshPage() {
        if($scope.pagination.current !== 1) {
          $scope.pagination.current = 1; //pageChanged event triggers reload
        } else {
          loadPage(1);
        }
      }

      $scope.onSortColumn = function(column, order) {
        $scope.sort.column = column.replaceAll('__locale__', getValidLocale()) || 'id';
        $scope.sort.order = order || 'asc';
        loadPage($scope.pagination.current);
      };

      $scope.$watch('pagination.searchText', function(newVal, oldVal) {
        if (!newVal && !oldVal) {
          return;
        }

        if(currentSearch) {
          $timeout.cancel(currentSearch);
        }

        currentSearch = $timeout(function() {
          refreshPage();
        }, 500);
      });

      function update() {
        $scope.filter = EntityStateFilterService.getFilterAndValidateUrl();
        loadPage($scope.pagination.current);
      }

      const searchFields = [
        {field: 'id', trKey: 'id'},
        {field: 'title', trKey: 'title', localized: true},
        {field: 'all', trKey: 'all'},
      ];

      $scope.$on('$locationChangeSuccess', () => update());
      $scope.loading = true;
      $scope.pagination = {current: 1, searchText: ''};
      $scope.totalCount = 0;
      $scope.limit = 3;
      $scope.sort = {
        column: `id`,
        order: 'asc'
      };
      $scope.search = {
        fields: searchFields,
        defaultField: searchFields[0],
        queryField: searchFields[0]
      };

      function getValidLocale() {
        const locale = $translate.use();
        return $scope.micaConfig.languages.indexOf(locale) > -1 ? locale : 'en';
      }

      MicaConfigResource.get().$promise.then((config) => {
        $scope.micaConfig = config;
        update();
      });

      update();
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
    'SfOptionsService',
    '$timeout',

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
      DocumentPermissionsService,
      SfOptionsService,
      $timeout) {

      function initializeForm() {
        MicaConfigResource.get(function (micaConfig) {
          $scope.tabs = [];
          micaConfig.languages.forEach(function (lang) {
            $scope.tabs.push({lang: lang});
          });
          $scope.languages = micaConfig.languages;
          var formLanguages = {};
          micaConfig.languages.forEach(function (loc) {
            formLanguages[loc] = $filter('translate')('language.' + loc);
          });

          SfOptionsService.transform().then(function(options) {
            $scope.sfOptions = options;
            $scope.sfOptions.formDefaults = {languages: formLanguages};
          });

          $scope.roles = micaConfig.roles;
          $scope.openAccess = micaConfig.openAccess;

          ProjectFormResource.get({ locale: $translate.use() },
            function onSuccess(projectForm) {
              $scope.form.definition = JsonUtils.parseJsonSafely(projectForm.definition, []);
              $scope.form.schema = JsonUtils.parseJsonSafely(projectForm.schema, {});
              $scope.form.schema.readonly = true;

              $timeout(function () { $scope.form = angular.copy($scope.form); }, 250);
            });
        });
      }

      function initializeProject(project) {
        $scope.permissions = DocumentPermissionsService.state(project['obiba.mica.EntityStateDto.projectState']);
      }

      $scope.Mode = {View: 0, Revision: 1, File: 2, Permission: 3, Comment: 4};

      $scope.form = {};

      initializeForm();

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

      var viewDiff = function (id, leftCommitInfo, rightCommitInfo) {
        if (leftCommitInfo && rightCommitInfo) {
          return DraftProjectRevisionsResource.diff({id: id, left: leftCommitInfo.commitId, right: rightCommitInfo.commitId, locale: $translate.use()});
        }
      };

      var restoreFromFields = function (transformFn) {
        DraftProjectResource.rGet({id: $scope.projectId}).$promise.then(function (project) {
          return transformFn(project.toJSON());
        }).then(function (result) {
          DraftProjectResource.rSave({id: $scope.projectId, comment: 'Restored Fields'}, result).$promise.then(function () {
            location.reload();
          }).catch(function (response) {
            $rootScope.$broadcast(NOTIFICATION_EVENTS.showNotificationDialog, {
              message: response.data ? response.data : angular.fromJson(response)
            });
          });

          return result;
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

      $scope.print = function () {
        setTimeout(function(){ window.print();}, 250);
      };

      $scope.fetchProject = fetchProject;
      $scope.viewRevision = viewRevision;
      $scope.restoreRevision = restoreRevision;
      $scope.fetchRevisions = fetchRevisions;
      $scope.restoreFromFields = restoreFromFields;
      $scope.publish = publish;
      $scope.viewDiff = viewDiff;

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

      $rootScope.$on('$translateChangeSuccess', function () {
        initializeForm();
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
    '$translate',
    '$filter',
    'DraftProjectResource',
    'DraftProjectsResource',
    'DraftProjectPublicationResource',
    'MicaConfigResource',
    'ProjectFormResource',
    'LocalizedValues',
    'JsonUtils',
    'FormServerValidation',
    'FormDirtyStateObserver',
    'SfOptionsService',
    '$timeout',
    function ($rootScope,
              $scope,
              $routeParams,
              $log,
              $locale,
              $location,
              $translate,
              $filter,
              DraftProjectResource,
              DraftProjectsResource,
              DraftProjectPublicationResource,
              MicaConfigResource,
              ProjectFormResource,
              LocalizedValues,
              JsonUtils,
              FormServerValidation,
              FormDirtyStateObserver,
              SfOptionsService,
              $timeout) {

      $scope.form = {};
      $scope.files = [];
      $scope.revision = {comment: null};
      $scope.newProject= !$routeParams.id;
      $scope.project = $routeParams.id ?
        DraftProjectResource.get({id: $routeParams.id}) : {published: false, model:{}};

      function initializeForm() {
        MicaConfigResource.get(function (micaConfig) {
          $scope.isCommentsRequiredOnDocumentSave = micaConfig.isCommentsRequiredOnDocumentSave;
          $scope.tabs = [];
          micaConfig.languages.forEach(function (lang) {
            $scope.tabs.push({lang: lang});
          });

          SfOptionsService.transform().then(function(options) {
            $scope.sfOptions = options;
            $scope.sfOptions.formDefaults = {languages: formLanguages};
          });

          var formLanguages = {};
          micaConfig.languages.forEach(function (loc) {
            formLanguages[loc] = $filter('translate')('language.' + loc);
          });
          $scope.sfOptions = {formDefaults: {languages: formLanguages}};
          ProjectFormResource.get({locale: $translate.use()},
            function onSuccess(projectForm) {
              $scope.form.definition = JsonUtils.parseJsonSafely(projectForm.definition, []);
              $scope.form.schema = JsonUtils.parseJsonSafely(projectForm.schema, {});

              $timeout(function () { $scope.form = angular.copy($scope.form); }, 250);
            });
        });
      }

      $rootScope.$on('$translateChangeSuccess', function () {
        initializeForm();
      });

      initializeForm();

      $scope.save = function () {
        $scope.$broadcast('schemaFormValidate');
        if ($scope.projectForm.$valid) {
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
        $scope.project.$save({comment: $scope.revision.comment},
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
