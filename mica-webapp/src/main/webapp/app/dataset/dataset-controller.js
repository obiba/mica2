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

/* global OPAL_TABLE_SCHEMA */
/* global OPAL_TABLE_DEFINITION */

/* global location */

mica.dataset.OPAL_TABLE_TYPES = {STUDY_TABLE: 'studyTable', HARMONIZATION_TABLE: 'harmonizationTable'};

mica.dataset

  .controller('CollectedDatasetListController', [
    '$scope', '$timeout', '$translate', 'CollectedDatasetsResource', 'DatasetService', 'AlertBuilder', 'EntityStateFilterService', 'MicaConfigResource', mica.commons.ListController
  ])

  .controller('CollectedDatasetEditController', ['$rootScope',
    '$scope',
    '$routeParams',
    '$log',
    '$locale',
    '$location',
    '$translate',
    '$filter',
    'CollectedDatasetResource',
    'DraftCollectedDatasetsResource',
    'CollectedDatasetPublicationResource',
    'MicaConfigResource',
    'FormServerValidation',
    'StudyStatesResource',
    'StudyStateProjectsResource',
    'FormDirtyStateObserver',
    'EntityFormResource',
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
              CollectedDatasetResource,
              DraftCollectedDatasetsResource,
              CollectedDatasetPublicationResource,
              MicaConfigResource,
              FormServerValidation,
              StudyStatesResource,
              StudyStateProjectsResource,
              FormDirtyStateObserver,
              EntityFormResource,
              SfOptionsService,
              $timeout) {
      $scope.studies = [];
      $scope.projects = [];
      $scope.selected = {};
      $scope.studyTable = {};
      $scope.revision = {comment: null};


      function initializeForm() {
        MicaConfigResource.get(function (micaConfig) {
          $scope.isCommentsRequiredOnDocumentSave = micaConfig.isCommentsRequiredOnDocumentSave;

          var formLanguages = {};
          micaConfig.languages.forEach(function (loc) {
            formLanguages[loc] = $filter('translate')('language.' + loc);
          });

          SfOptionsService.transform().then(function(options) {
            $scope.sfOptions = options;
            $scope.sfOptions.formDefaults = {languages: formLanguages};
          });

          EntityFormResource.get({target: 'collected-dataset', locale: $translate.use()}, function (form) {
            form.schema = angular.fromJson(form.schema);
            form.definition = angular.fromJson(form.definition);
            $scope.sfForm = form;

            $timeout(function () { $scope.sfForm = angular.copy(form); }, 250);
          });

        });
      }

      var getTypeFromUrl = function() {
        var matched = /\/(\w+-dataset)\//.exec($location.path());
        return matched ? matched[1] : '';
      };

      var updateDataset = function () {
        $scope.dataset.$save({comment: $scope.revision.comment},
          function (dataset) {
            FormDirtyStateObserver.unobserve();
            $location.path('/collected-dataset/' + dataset.id).replace();
          },
          saveErrorHandler);
      };

      var createDataset = function () {
        DraftCollectedDatasetsResource.save($scope.dataset,
          function (resource, getResponseHeaders) {
            FormDirtyStateObserver.unobserve();
            var parts = getResponseHeaders().location.split('/');
            $location.path('/collected-dataset/' + parts[parts.length - 1]).replace();
          },
          saveErrorHandler);
      };

      var saveErrorHandler = function (response) {
        FormServerValidation.error(response, $scope.form, $scope.languages);
      };

      var populateStudyTable = function (studyTable) {
        if (studyTable !== {}) {
          $scope.selected.study = {
            id: studyTable.studyId,
            population: {
              id: studyTable.populationId,
              dataCollectionEvent: {id: studyTable.dataCollectionEventId}
            }
          };

          $scope.selected.project = {name: studyTable.project, table: studyTable.table};
        }

        StudyStatesResource.query().$promise.then(function (studies) {
          $scope.studies = studies.sort(function (a, b) { return a.id.localeCompare(b.id); });

          var selectedPopulation, selectedDce, selectedStudy = $scope.studies.filter(function (s) {return s.id === studyTable.studyId; })[0];

          if (selectedStudy) {
            $scope.selected.study = selectedStudy;
            selectedPopulation = selectedStudy.populationSummaries.filter(function (p) { return p.id === studyTable.populationId; })[0];

            if (selectedPopulation) {
              $scope.selected.study.population = selectedPopulation;

              selectedDce = selectedPopulation.dataCollectionEventSummaries.filter(function (dce) { return dce.id === studyTable.dataCollectionEventId; })[0];

              if (selectedDce) {
                $scope.selected.study.population.dataCollectionEvent = selectedDce;
              }
            }
          }
        });
      };

      $scope.type = getTypeFromUrl();
      $scope.newDataset = !$routeParams.id;
      if ($routeParams.id) {
        $scope.dataset = CollectedDatasetResource.get({id: $routeParams.id}, function (dataset) {
          $scope.studyTable = dataset['obiba.mica.CollectedDatasetDto.type'].studyTable;
          populateStudyTable($scope.studyTable);
        });
      } else {
        $scope.dataset = {
          published: false,
          model: {}
        };

        populateStudyTable($scope.studyTable);
      }

      $scope.$watch('selected.study', function () {
        if ($scope.selected.study && $scope.selected.study.id) {
          StudyStateProjectsResource.query({id: $scope.selected.study.id}).$promise.then(function (projects) {
            $scope.projects = projects;
            var selectedTable, selectedProject = $scope.projects.filter(function (p) {return p.name === $scope.studyTable.project; })[0];

            if (selectedProject) {
              $scope.selected.project = selectedProject;

              selectedTable = selectedProject.datasource.table.filter(function (t) {return t === $scope.studyTable.table; })[0];

              if (selectedTable) {
                $scope.selected.project.table = selectedTable;
              }
            }
          });
        }
      });

      $scope.save = function () {
        $scope.$broadcast('schemaFormValidate');
        if ($scope.form.$valid) {
          if ($scope.dataset.id) {
            updateDataset();
          } else {
            createDataset();
          }
        } else {
          $scope.form.saveAttempted = true;
        }
      };

      $scope.cancel = function () {
        $location.path('/collected-dataset' + ($scope.dataset.id ? '/' + $scope.dataset.id : '')).replace();
      };

      $rootScope.$on('$translateChangeSuccess', function () {
        initializeForm();
      });

      initializeForm();

      FormDirtyStateObserver.observe($scope);
    }])

  .controller('HarmonizedDatasetEditController', ['$rootScope',
    '$scope',
    '$routeParams',
    '$log',
    '$location',
    '$translate',
    '$filter',
    'HarmonizedDatasetResource',
    'DraftHarmonizedDatasetsResource',
    'MicaConfigResource',
    'FormServerValidation',
    'StudyStatesResource',
    'MicaConfigOpalProjectsResource',
    'FormDirtyStateObserver',
    'EntityFormResource',
    'OpalTablesService',
    'SfOptionsService',
    '$timeout',

    function ($rootScope,
              $scope,
              $routeParams,
              $log,
              $location,
              $translate,
              $filter,
              HarmonizedDatasetResource,
              DraftHarmonizedDatasetsResource,
              MicaConfigResource,
              FormServerValidation,
              StudyStatesResource,
              MicaConfigOpalProjectsResource,
              FormDirtyStateObserver,
              EntityFormResource,
              OpalTablesService,
              SfOptionsService,
              $timeout) {


      function initializeForm() {
        MicaConfigResource.get(function (micaConfig) {
          $scope.isCommentsRequiredOnDocumentSave = micaConfig.isCommentsRequiredOnDocumentSave;

          var formLanguages = {};
          micaConfig.languages.forEach(function (loc) {
            formLanguages[loc] = $filter('translate')('language.' + loc);
          });

          SfOptionsService.transform().then(function(options) {
            $scope.sfOptions = options;
            $scope.sfOptions.formDefaults = {languages: formLanguages};
          });

          EntityFormResource.get({target: 'harmonized-dataset', locale: $translate.use()}, function (form) {
            form.schema = angular.fromJson(form.schema);
            form.definition = angular.fromJson(form.definition);
            $scope.sfForm = form;

            $timeout(function () { $scope.sfForm = angular.copy(form); }, 250);
          });
        });
      }

      var getTypeFromUrl = function() {
        var matched = /\/(\w+-dataset)\//.exec($location.path());
        return matched ? matched[1] : '';
      };

      $scope.selected = {};
      $scope.harmonizationStudies = StudyStatesResource.query({type: 'harmonization-study'});
      $scope.type = getTypeFromUrl();
      $scope.newDataset = !$routeParams.id;
      $scope.revision = {comment: null};

      function getOpalProjects() {
        return MicaConfigOpalProjectsResource.get().$promise.then(function(projects){
          $scope.projects = projects;
        });
      }

      if ($routeParams.id) {
        $scope.dataset = HarmonizedDatasetResource.get({id: $routeParams.id});
        $scope.dataset.$promise.then(function (dataset) {

          $scope.opalTables = OpalTablesService.getTables(dataset);

          $scope.harmonizationStudies.$promise.then(function (harmonizationStudies) {
            $scope.selected.study = harmonizationStudies.filter(function (s) {
              return dataset['obiba.mica.HarmonizedDatasetDto.type'].harmonizationTable &&
                s.id === dataset['obiba.mica.HarmonizedDatasetDto.type'].harmonizationTable.studyId;
            })[0];

          });

          getOpalProjects().then(function() {
            $scope.selected.project = $scope.projects.filter(function (p) {
              return p.name === dataset['obiba.mica.HarmonizedDatasetDto.type'].harmonizationTable.project;
            }).pop();

            if ($scope.selected.project) {
              $scope.selected.project.table = $scope.selected.project.datasource.table.filter(function (t) {
                return t === dataset['obiba.mica.HarmonizedDatasetDto.type'].harmonizationTable.table;
              }).pop();
            }
          });
        });
      } else {
        getOpalProjects();
        $scope.dataset = {
          published: false,
          'obiba.mica.HarmonizedDatasetDto.type': {},
          model: {}
        };
      }

      $scope.save = function () {

        $scope.$broadcast('schemaFormValidate');

        if (!$scope.form.$valid) {
          $scope.form.saveAttempted = true;
          return;
        }

        $scope.dataset['obiba.mica.HarmonizedDatasetDto.type'].harmonizationTable = $scope.dataset['obiba.mica.HarmonizedDatasetDto.type'].harmonizationTable || {};
        $scope.dataset['obiba.mica.HarmonizedDatasetDto.type'].harmonizationTable.project = $scope.selected.project.name;
        $scope.dataset['obiba.mica.HarmonizedDatasetDto.type'].harmonizationTable.table = $scope.selected.project.table;
        $scope.dataset['obiba.mica.HarmonizedDatasetDto.type'].harmonizationTable.studyId = $scope.selected.study ? $scope.selected.study.id : null;

        if ($scope.dataset.id) {
          updateDataset();
        } else {
          createDataset();
        }
      };

      $scope.cancel = function () {
        $location.path('/harmonized-dataset' + ($scope.dataset.id ? '/' + $scope.dataset.id : '')).replace();
      };

      var updateDataset = function () {
        $scope.dataset.$save({comment: $scope.revision.comment},
          function (dataset) {
            FormDirtyStateObserver.unobserve();
            $location.path('/harmonized-dataset/' + dataset.id).replace();
          },
          saveErrorHandler);
      };

      var createDataset = function () {
        DraftHarmonizedDatasetsResource.save($scope.dataset,
          function (resource, getResponseHeaders) {
            FormDirtyStateObserver.unobserve();
            var parts = getResponseHeaders().location.split('/');
            $location.path('/harmonized-dataset/' + parts[parts.length - 1]).replace();
          },
          saveErrorHandler);
      };

      var saveErrorHandler = function (response) {
        FormServerValidation.error(response, $scope.form, $scope.languages);
      };

      $rootScope.$on('$translateChangeSuccess', function () {
        initializeForm();
      });

      initializeForm();
      FormDirtyStateObserver.observe($scope);
    }])

  .controller('DatasetViewController', ['$rootScope',
    '$scope',
    '$routeParams',
    '$log',
    '$locale',
    '$location',
    '$translate',
    '$uibModal',
    '$filter',
    'DatasetResource',
    'DatasetPublicationResource',
    'DraftDatasetStatusResource',
    'DraftDatasetViewRevisionResource',
    'DraftDatasetRevisionsResource',
    'DraftDatasetRestoreRevisionResource',
    'DraftFileSystemSearchResource',
    'MicaConfigResource',
    'NOTIFICATION_EVENTS',
    'DatasetService',
    'DocumentPermissionsService',
    'StudyStatesResource',
    'EntityFormResource',
    'OpalTablesService',
    'CollectedDatasetResource',
    'HarmonizedDatasetResource',
    'SfOptionsService',
    'AlertBuilder',
    '$timeout',

    function ($rootScope,
              $scope,
              $routeParams,
              $log,
              $locale,
              $location,
              $translate,
              $uibModal,
              $filter,
              DatasetResource,
              DatasetPublicationResource,
              DraftDatasetStatusResource,
              DraftDatasetViewRevisionResource,
              DraftDatasetRevisionsResource,
              DraftDatasetRestoreRevisionResource,
              DraftFileSystemSearchResource,
              MicaConfigResource,
              NOTIFICATION_EVENTS,
              DatasetService,
              DocumentPermissionsService,
              StudyStatesResource,
              EntityFormResource,
              OpalTablesService,
              CollectedDatasetResource,
              HarmonizedDatasetResource,
              SfOptionsService,
              AlertBuilder,
              $timeout) {

      function initializeForm() {
        MicaConfigResource.get(function (micaConfig) {
          $scope.isCommentsRequiredOnDocumentSave = micaConfig.isCommentsRequiredOnDocumentSave;

          $scope.opal = micaConfig.opal;
          var formLanguages = {};
          micaConfig.languages.forEach(function (loc) {
            formLanguages[loc] = $filter('translate')('language.' + loc);
          });

          SfOptionsService.transform().then(function(options) {
            $scope.sfOptions = options;
            $scope.sfOptions.pristine = {errors: true, success: false};
            $scope.sfOptions.formDefaults = {languages: formLanguages};
          });

          $scope.openAccess = micaConfig.openAccess;

          EntityFormResource.get({target: $scope.type, locale: $translate.use()}, function (form) {
            form.schema = angular.fromJson(form.schema);
            form.schema.readonly = true;
            form.definition = angular.fromJson(form.definition);
            $scope.sfForm = form;

            $timeout(function () { $scope.sfForm = angular.copy(form); }, 250);
          });
        });
      }

      function addUpdateOpalTable(wrapper) {

        $uibModal
          .open({
            templateUrl: 'app/dataset/views/opal-table-modal-form.html',
            controller: 'StudyTableModalController',
            size: 'lg',
            resolve: {
              isCommentsRequiredOnDocumentSave: $scope.isCommentsRequiredOnDocumentSave,
              table: function() {
                if($scope.type === 'harmonized-dataset') {
                  return angular.isDefined(wrapper) ? wrapper.table : {weight: $scope.opalTables ? $scope.opalTables.length : 0};
                } else {
                  return angular.isDefined($scope.dataset['obiba.mica.CollectedDatasetDto.type']) ? $scope.dataset['obiba.mica.CollectedDatasetDto.type'].studyTable : {};
                }
              },
              tableType: function () {
                if($scope.type === 'harmonized-dataset') {
                  return wrapper ? wrapper.type : mica.dataset.OPAL_TABLE_TYPES.STUDY_TABLE;
                } else {
                  return mica.dataset.OPAL_TABLE_TYPES.STUDY_TABLE;
                }
              },
              isHarmonizedDatasetScope: $scope.type === 'harmonized-dataset'
            }
          })
          .result.then(
            function (data) {
              if($scope.type === 'harmonized-dataset') {
                OpalTablesService.addUpdateTable($scope.dataset, data.type, wrapper, data.table);
              } else {
                OpalTablesService.setTable($scope.dataset, data.table);
              }
              saveAndUpdateDataset(data.table.comment);
            },
            function (what) {
              $log.error(what);
            }
          );
      }

      var initializeDataset = function(dataset) {
        $scope.selectedLocale = $translate.use();

        $scope.permissions = DocumentPermissionsService.state(dataset['obiba.mica.EntityStateDto.datasetState']);
        if($scope.type === 'harmonized-dataset') {
          if(dataset['obiba.mica.HarmonizedDatasetDto.type'].harmonizationTable) {
            StudyStatesResource.get({id: dataset['obiba.mica.HarmonizedDatasetDto.type'].harmonizationTable.studyId}).$promise.then(function (harmonizationStudy) {
              $scope.datasetStudy = harmonizationStudy.id;
              $scope.datasetPopulation = harmonizationStudy.populationSummaries.filter(function (population) {
                return population.id === dataset['obiba.mica.HarmonizedDatasetDto.type'].harmonizationTable.populationId;
              })[0];
            }).catch(function () {});
          }
          $scope.datasetProject = dataset['obiba.mica.HarmonizedDatasetDto.type'].harmonizationTable.project;
          $scope.datasetTable = dataset['obiba.mica.HarmonizedDatasetDto.type'].harmonizationTable.table;
          $scope.opalTables = OpalTablesService.getTables(dataset);

          $scope.addStudyTable = function () {
            addUpdateOpalTable();
          };

          $scope.editOpalTable = function (wrapper) {
            addUpdateOpalTable(wrapper);
          };

          $scope.deleteOpalTable = function (wrapper) {
            OpalTablesService.deleteTable($scope.dataset, wrapper);
            saveAndUpdateDataset();
          };

          $scope.deleteSelectedOpalTables = function () {
            var wrappers = $scope.opalTables.filter(function (t) {
              return t.selected;
            });
            OpalTablesService.deleteTables($scope.dataset, wrappers);
            saveAndUpdateDataset();
          };

          $scope.moveOpalTableDown = function (index) {
            var studyToMoveDown = $scope.opalTables[index];
            var studyToMoveUp = $scope.opalTables[index + 1];

            $scope.opalTables[index] = studyToMoveUp;
            $scope.opalTables[index + 1] = studyToMoveDown;

            OpalTablesService.updateWeights($scope.opalTables);

            if ($scope.isCommentsRequiredOnDocumentSave) {
              $uibModal.open({
                templateUrl: 'app/comment/views/add-comment-modal.html',
                controller: ['$scope', '$uibModalInstance', function (scope, $uibModalInstance) {
                  scope.ok = function (comment) {
                    $uibModalInstance.close(comment);
                  };

                  scope.cancel = function () {
                    $uibModalInstance.dismiss('cancel');
                  };
                }]
              }).result.then(function (comment) {
                saveAndUpdateDataset(comment);
              });
            } else {
              saveAndUpdateDataset();
            }
          };

          $scope.moveOpalTableUp = function (index) {
            $scope.moveOpalTableDown(index - 1);
          };

        } else {
          $scope.editStudyTable = function () {
            addUpdateOpalTable();
          };
          $scope.deleteStudyTable = function () {
            delete $scope.dataset['obiba.mica.CollectedDatasetDto.type'];
            saveAndUpdateDataset();
          };
        }

        $scope.dataset = dataset;
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

      var viewRevision = function (datasetId, commitInfo) {
        $scope.commitInfo = commitInfo;
        DraftDatasetViewRevisionResource.view({
          id: datasetId,
          commitId: commitInfo.commitId,
          type: $scope.type
        }).$promise.then(function (dataset) {
          $scope.dataset = dataset;
          $scope.opalTables = OpalTablesService.getTables(dataset);
        });
      };

      $scope.viewDiff = function (id, leftCommitInfo, rightCommitInfo) {
        if (leftCommitInfo && rightCommitInfo) {
          return DraftDatasetRevisionsResource.diff({id: id, type: $scope.type, left: leftCommitInfo.commitId, right: rightCommitInfo.commitId, locale: $translate.use()});
        }
      };

      var fetchDataset = function (datasetId) {
        DatasetResource.get({id: datasetId, type: $scope.type}, initializeDataset);
      };

      var fetchRevisions = function (datasetId, onSuccess) {
        DraftDatasetRevisionsResource.query({id: datasetId, type: $scope.type}, function (response) {
          if (onSuccess) {
            onSuccess(response);
          }
        });
      };

      var restoreFromFields = function (transformFn) {
        DatasetResource.rGet({id: $scope.datasetId, type: $scope.type}).$promise.then(function (dataset) {
          return transformFn(dataset.toJSON());
        }).then(function (result) {
          DatasetResource.rSave({id: $scope.datasetId, type: $scope.type, comment: 'Restored Fields'}, result).$promise.then(function () {
            location.reload();
          }).catch(function (response) {
            $rootScope.$broadcast(NOTIFICATION_EVENTS.showNotificationDialog, {
              message: response.data ? response.data : angular.fromJson(response)
            });
          });

          return result;
        });
      };

      function saveAndUpdateDataset(comment) {
        var payload = {id: $scope.dataset.id};
        if (comment) {
          payload.comment = comment;
        }

        if($scope.type === 'harmonized-dataset') {
          HarmonizedDatasetResource.save(payload, $scope.dataset).$promise.then(function () {
            fetchDataset($scope.dataset.id);
          });
        } else {
          CollectedDatasetResource.save(payload, $scope.dataset).$promise.then(function () {
            fetchDataset($scope.dataset.id);
          });
        }
      }

      var restoreRevision = function (datasetId, commitInfo, onSuccess) {
        if (commitInfo && $scope.datasetId === datasetId) {
          var args = {commitId: commitInfo.commitId, restoreSuccessCallback: onSuccess};

          $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
            {
              titleKey: 'dataset.restore-dialog.title',
              messageKey: 'dataset.restore-dialog.message',
              messageArgs: [$filter('amDateFormat')(commitInfo.date, 'lll')]
            }, args
          );
        }
      };

      var onRestore = function (event, args) {
        if (args.commitId) {
          DraftDatasetRestoreRevisionResource.restore({id: $scope.datasetId, commitId: args.commitId, type: $scope.type},
            function () {
              fetchDataset($routeParams.id);
              $scope.datasetId = $routeParams.id;
              if (args.restoreSuccessCallback) {
                args.restoreSuccessCallback();
              }
            });
        }
      };

      var getTypeFromUrl = function() {
        var matched = /\/(\w+-dataset)\//.exec($location.path());
        return matched ? matched[1] : '';
      };

      $scope.Mode = {View: 0, Revision: 1, File: 2, Permission: 3, Comment: 4};
      $scope.type = getTypeFromUrl();
      $scope.datasetId = $routeParams.id;
      DatasetResource.get({id: $routeParams.id, type: $scope.type}, initializeDataset);

      $scope.publish = function (publish) {
        if (publish) {
          DraftFileSystemSearchResource.searchUnderReview({path: '/' + $scope.type + '/' + $scope.dataset.id},
            function onSuccess(response) {
              DatasetPublicationResource.handledPublish(
                {id: $scope.dataset.id, type: $scope.type, cascading: response.length > 0 ? 'UNDER_REVIEW' : 'NONE'},
                function () {
                  DatasetResource.get({id: $routeParams.id, type: $scope.type}, initializeDataset);
                },
                function (error) {
                  AlertBuilder.newBuilder().trMsg(error.data).build();
                });
            },
            function onError() {
              $log.error('Failed to search for Under Review files.');
            }
          );
        } else {
          DatasetPublicationResource.unPublish({id: $scope.dataset.id, type: $scope.type}, function () {
            DatasetResource.get({id: $routeParams.id, type: $scope.type}, initializeDataset);
          });
        }
      };

      $scope.toStatus = function (value) {
        DraftDatasetStatusResource.toStatus({id: $scope.dataset.id, type:$scope.type, value: value}, function () {
          DatasetResource.get({id: $routeParams.id, type: $scope.type}, initializeDataset);
        });
      };

      $scope.delete = function () {
        $scope.dataset.type = $scope.type;
        DatasetService.delete($scope.dataset, function () {
          $location.path('/' + $scope.type);
        });
      };

      $scope.viewMode = getViewMode();
      $scope.inViewMode = function () {
        return $scope.viewMode === $scope.Mode.View;
      };
      $scope.fetchDataset = fetchDataset;
      $scope.viewRevision = viewRevision;
      $scope.restoreRevision = restoreRevision;
      $scope.fetchRevisions = fetchRevisions;
      $scope.restoreFromFields = restoreFromFields;

      $scope.print = function () {
        setTimeout(function(){ window.print();}, 250);
      };

      $rootScope.$on('$translateChangeSuccess', function () {
        initializeForm();
      });

      initializeForm();

      $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, onRestore);
    }])

  .controller('DatasetPermissionsController', ['$scope', '$routeParams', 'DraftDatasetPermissionsResource', 'DraftDatasetAccessesResource',
    function ($scope, $routeParams, DraftDatasetPermissionsResource, DraftDatasetAccessesResource) {
    $scope.permissions = [];
    $scope.accesses = [];

    $scope.loadPermissions = function () {
      $scope.permissions = DraftDatasetPermissionsResource.query({id: $routeParams.id, datasetType: $scope.type});
      return $scope.permissions;
    };

    $scope.addPermission = function (permission) {
      return DraftDatasetPermissionsResource.save({id: $routeParams.id, datasetType: $scope.type}, permission);
    };

    $scope.deletePermission = function(permission) {
      return DraftDatasetPermissionsResource.delete({id: $routeParams.id, datasetType: $scope.type}, permission);
    };

    $scope.loadAccesses = function () {
      $scope.accesses = DraftDatasetAccessesResource.query({id: $routeParams.id, datasetType: $scope.type});
      return $scope.accesses;
    };

    $scope.deleteAccess = function (access) {
      return DraftDatasetAccessesResource.delete({id: $routeParams.id, datasetType: $scope.type}, access);
    };

    $scope.addAccess = function (access) {
      return DraftDatasetAccessesResource.save({id: $routeParams.id, datasetType: $scope.type}, access);
    };
  }])

  .controller('HarmonizedDatasetListController', [
    '$scope', '$timeout', '$translate', 'HarmonizedDatasetsResource', 'DatasetService', 'AlertBuilder', 'EntityStateFilterService', 'MicaConfigResource', mica.commons.ListController
  ])

  .controller('StudyTableModalController', [
    '$scope',
    '$uibModalInstance',
    '$log',
    '$filter',
    'MicaConfigResource',
    'StudyStatesResource',
    'StudyStateProjectsResource',
    'LocalizedValues',
    'LocalizedSchemaFormService',
    'isCommentsRequiredOnDocumentSave',
    'table',
    'tableType',
    'isHarmonizedDatasetScope',
    function ($scope,
              $uibModalInstance,
              $log,
              $filter,
              MicaConfigResource,
              StudyStatesResource,
              StudyStateProjectsResource,
              LocalizedValues,
              LocalizedSchemaFormService,
              isCommentsRequiredOnDocumentSave,
              table,
              tableType,
              isHarmonizedDatasetScope) {

      $scope.studies = [];
      $scope.projects = [];
      $scope.isHarmonizedDatasetScope = isHarmonizedDatasetScope;
      $scope.isCommentsRequiredOnDocumentSave = isCommentsRequiredOnDocumentSave;
      $scope.selected = {
        get isHarmonizationTable() {
          return this._isHarmonizationTable;
        },
        set isHarmonizationTable(value) {
          this._isHarmonizationTable = value;
          $scope.selected.study = null;
          $scope.type = value ? mica.dataset.OPAL_TABLE_TYPES.HARMONIZATION_TABLE : mica.dataset.OPAL_TABLE_TYPES.STUDY_TABLE;
        }
      };
      $scope.type = tableType;
      $scope.selected.isHarmonizationTable = tableType === mica.dataset.OPAL_TABLE_TYPES.HARMONIZATION_TABLE;
      $scope.table = $.extend(true, {}, table);
      $scope.table.model = {
        name: LocalizedValues.arrayToObject(table.name),
        description: LocalizedValues.arrayToObject(table.description),
        additionalInformation: LocalizedValues.arrayToObject(table.additionalInformation)
      };

      MicaConfigResource.get(function (micaConfig) {
        var formLanguages = {};
        micaConfig.languages.forEach(function(loc) {
          formLanguages[loc] = $filter('translate')('language.' + loc);
        });
        $scope.sfOptions = {formDefaults: { languages: formLanguages}};
        $scope.sfForm = {
          schema: LocalizedSchemaFormService.translate(OPAL_TABLE_SCHEMA),
          definition: LocalizedSchemaFormService.translate(OPAL_TABLE_DEFINITION)
        };
      });

      if (table && Object.keys(table) > 0) {
        $scope.selected.study = {
          id: table.studyId,
          population: {
            id: table.populationId,
            dataCollectionEvent: {id: table.dataCollectionEventId}
          }
        };

        $scope.selected.project = {name: table.project, table: table.table};
      }

      StudyStatesResource.query().$promise.then(function (studies) {
        $scope.studies = studies.sort(function (a, b) { return a.id.localeCompare(b.id); });

        var selectedPopulation, selectedDce, selectedStudy = $scope.studies.filter(function (s) {return s.id === table.studyId; })[0];

        if (selectedStudy) {
          $scope.selected.study = selectedStudy;
          selectedPopulation = selectedStudy.populationSummaries.filter(function (p) { return p.id === table.populationId; })[0];

          if (selectedPopulation) {
            $scope.selected.study.population = selectedPopulation;

            selectedDce = selectedPopulation.dataCollectionEventSummaries.filter(function (dce) { return dce.id === table.dataCollectionEventId; })[0];

            if (selectedDce) {
              $scope.selected.study.population.dataCollectionEvent = selectedDce;
            }
          }
        }
      });

      StudyStatesResource.query({type: 'harmonization-study'}).$promise.then(function (studies) {
        $scope.harmonizationStudies = studies.sort(function (a, b) { return a.id.localeCompare(b.id); });

        var selectedPopulation, selectedStudy = $scope.harmonizationStudies.filter(function (s) {return s.id === table.studyId; })[0];

        if (selectedStudy) {
          $scope.selected.study = selectedStudy;
          selectedPopulation = selectedStudy.populationSummaries.filter(function (p) { return p.id === table.populationId; })[0];

          if (selectedPopulation) {
            $scope.selected.study.population = selectedPopulation;
          }
        }
      });

      $scope.$watch('selected.study', function () {
        if ($scope.selected.study && $scope.selected.study.id) {
          StudyStateProjectsResource.query({id: $scope.selected.study.id}).$promise.then(function (projects) {
            $scope.projects = projects;
            var selectedTable, selectedProject = $scope.projects.filter(function (p) {return p.name === table.project; })[0];

            if (selectedProject) {
              $scope.selected.project = selectedProject;

              selectedTable = selectedProject.datasource.table.filter(function (t) {return t === table.table; })[0];

              if (selectedTable) {
                $scope.selected.project.table = selectedTable;
              }
            }

          });
        }
      });

      $scope.save = function (form) {
        if (form.$valid) {
          const populationId =
            $scope.selected.isHarmonizationTable || !$scope.selected.study.population ? null : $scope.selected.study.population.id;

          const dceId = $scope.selected.isHarmonizationTable ||
            !$scope.selected.study.population ||
            !$scope.selected.study.population.dataCollectionEvent ? null : $scope.selected.study.population.dataCollectionEvent.id;

          angular.extend($scope.table, {
            studyId: $scope.selected.study.id,
            populationId: populationId,
            dataCollectionEventId: dceId,
            project: $scope.selected.project.name,
            table: $scope.selected.project.table
          });

          $scope.table.name = LocalizedValues.objectToArray($scope.table.model.name);
          $scope.table.description = LocalizedValues.objectToArray($scope.table.model.description);
          $scope.table.additionalInformation = LocalizedValues.objectToArray($scope.table.model.additionalInformation);
          delete $scope.table.model;

          $uibModalInstance.close({table: $scope.table, type: $scope.type});
          return;
        }

        $scope.form = form;
        $scope.form.saveAttempted = true;
      };

      $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };

    }]);
