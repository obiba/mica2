'use strict';

mica.dataset

  .controller('StudyDatasetListController', ['$rootScope', '$scope', 'StudyDatasetsResource',
    'DatasetService',
    function ($rootScope, $scope, StudyDatasetsResource, DatasetService) {
      var onSuccess = function(response) {
        $scope.studyDatasets = response;
        $scope.loading = false;
      };

      var onError = function() {
        $scope.loading = false;
      };

      $scope.loading = true;
      StudyDatasetsResource.query({}, onSuccess, onError);
      $scope.deleteStudyDataset = function (dataset) {
        DatasetService.deleteDataset(dataset, function () {
          $scope.loading = true;
          StudyDatasetsResource.query({}, onSuccess, onError);
        });
      };
    }])

  .controller('StudyDatasetEditController', ['$rootScope',
    '$scope',
    '$routeParams',
    '$log',
    '$locale',
    '$location',
    'StudyDatasetResource',
    'DraftStudyDatasetsResource',
    'StudyDatasetPublicationResource',
    'MicaConfigResource',
    'FormServerValidation',
    'StudyStatesResource',
    'StudyStateProjectsResource',
    'ActiveTabService',

    function ($rootScope,
              $scope,
              $routeParams,
              $log,
              $locale,
              $location,
              StudyDatasetResource,
              DraftStudyDatasetsResource,
              StudyDatasetPublicationResource,
              MicaConfigResource,
              FormServerValidation,
              StudyStatesResource,
              StudyStateProjectsResource,
              ActiveTabService) {
      $scope.studies = [];
      $scope.projects = [];
      $scope.selected = {};
      $scope.studyTable = {};

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

      var getTypeFromUrl = function() {
        var matched = /\/(\w+-dataset)\//.exec($location.path());
        return matched ? matched[1] : '';
      };

      $scope.type = getTypeFromUrl();
      $scope.getActiveTab = ActiveTabService.getActiveTab;
      $scope.newDataset = !$routeParams.id;
      if ($routeParams.id) {
        $scope.dataset = StudyDatasetResource.get({id: $routeParams.id}, function (dataset) {
          $scope.studyTable = dataset['obiba.mica.StudyDatasetDto.type'].studyTable;
          populateStudyTable($scope.studyTable);
        });
      } else {
        $scope.dataset = {
          published: false, 'obiba.mica.StudyDatasetDto.type': {studyTable: {}}
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

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({lang: lang});
        });
      });

      $scope.save = function () {
        if (!$scope.form.$valid) {
          $scope.form.saveAttempted = true;
          return;
        }

        angular.extend($scope.dataset['obiba.mica.StudyDatasetDto.type'].studyTable, {
          studyId: $scope.selected.study.id,
          populationId: $scope.selected.study.population.id,
          dataCollectionEventId: $scope.selected.study.population.dataCollectionEvent.id,
          project: $scope.selected.project.name,
          table: $scope.selected.project.table
        });

        if ($scope.dataset.id) {
          updateDataset();
        } else {
          createDataset();
        }
      };

      $scope.cancel = function () {
        $location.path('/study-dataset' + ($scope.dataset.id ? '/' + $scope.dataset.id : '')).replace();
      };

      var updateDataset = function () {
        $scope.dataset.$save(
          function (dataset) {
            $location.path('/study-dataset/' + dataset.id).replace();
          },
          saveErrorHandler);
      };

      var createDataset = function () {
        DraftStudyDatasetsResource.save($scope.dataset,
          function (resource, getResponseHeaders) {
            var parts = getResponseHeaders().location.split('/');
            $location.path('/study-dataset/' + parts[parts.length - 1]).replace();
          },
          saveErrorHandler);
      };

      var saveErrorHandler = function (response) {
        FormServerValidation.error(response, $scope.form, $scope.languages);
      };

    }])

  .controller('HarmonizationDatasetEditController', ['$rootScope',
    '$scope',
    '$routeParams',
    '$log',
    '$locale',
    '$location',
    '$modal',
    'HarmonizationDatasetResource',
    'DraftHarmonizationDatasetsResource',
    'HarmonizationDatasetPublicationResource',
    'MicaConfigResource',
    'FormServerValidation',
    'ActiveTabService',

    function ($rootScope,
              $scope,
              $routeParams,
              $log,
              $locale,
              $location,
              $modal,
              HarmonizationDatasetResource,
              DraftHarmonizationDatasetsResource,
              HarmonizationDatasetPublicationResource,
              MicaConfigResource,
              FormServerValidation,
              ActiveTabService) {


      var getTypeFromUrl = function() {
        var matched = /\/(\w+-dataset)\//.exec($location.path());
        return matched ? matched[1] : '';
      };

      $scope.type = getTypeFromUrl();
      $scope.getActiveTab = ActiveTabService.getActiveTab;
      $scope.newDataset = !$routeParams.id;
      $scope.dataset = $routeParams.id ? HarmonizationDatasetResource.get({id: $routeParams.id}) : {
        published: false,
        'obiba.mica.HarmonizationDatasetDto.type': {}
      };

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({lang: lang});
        });
      });

      $scope.save = function () {
        if (!$scope.form.$valid) {
          $scope.form.saveAttempted = true;
          return;
        }
        if ($scope.dataset.id) {
          updateDataset();
        } else {
          createDataset();
        }
      };

      $scope.addStudyTable = function (tab) {
        $modal
          .open({
            templateUrl: 'app/dataset/views/study-table-modal-form.html',
            controller: 'StudyTableModalController',
            resolve: {
              studyTable: function () {
                return {};
              },
              tab : function () {
                return tab;
              }
            }
          })
          .result.then(function (studyTable) {
            if (!$scope.dataset['obiba.mica.HarmonizationDatasetDto.type'].studyTables) {
              $scope.dataset['obiba.mica.HarmonizationDatasetDto.type'].studyTables = [];
            }
            $scope.dataset['obiba.mica.HarmonizationDatasetDto.type'].studyTables.push(studyTable);
          }, function () {
          });
      };

      $scope.editStudyTable = function (index, tab) {
        $modal
          .open({
            templateUrl: 'app/dataset/views/study-table-modal-form.html',
            controller: 'StudyTableModalController',
            resolve: {
              tab: function () {
                return tab;
              },
              studyTable: function () {
                return $scope.dataset['obiba.mica.HarmonizationDatasetDto.type'].studyTables[index];
              }
            }
          })
          .result.then(function (studyTable) {
            $scope.dataset['obiba.mica.HarmonizationDatasetDto.type'].studyTables[index] = studyTable;
          }, function () {
          });
      };

      $scope.deleteStudyTable = function (index) {
        $scope.dataset['obiba.mica.HarmonizationDatasetDto.type'].studyTables.splice(index);
        if ($scope.dataset['obiba.mica.HarmonizationDatasetDto.type'].studyTables.length === 0) {
          $scope.dataset['obiba.mica.HarmonizationDatasetDto.type'].studyTables = undefined;
        }
      };

      $scope.cancel = function () {
        $location.path('/harmonization-dataset' + ($scope.dataset.id ? '/' + $scope.dataset.id : '')).replace();
      };

      var updateDataset = function () {
        $scope.dataset.$save(
          function (dataset) {
            $location.path('/harmonization-dataset/' + dataset.id).replace();
          },
          saveErrorHandler);
      };

      var createDataset = function () {
        DraftHarmonizationDatasetsResource.save($scope.dataset,
          function (resource, getResponseHeaders) {
            var parts = getResponseHeaders().location.split('/');
            $location.path('/harmonization-dataset/' + parts[parts.length - 1]).replace();
          },
          saveErrorHandler);
      };

      var saveErrorHandler = function (response) {
        FormServerValidation.error(response, $scope.form, $scope.languages);
      };

    }])

  .controller('DatasetViewController', ['$rootScope',
    '$scope',
    '$routeParams',
    '$log',
    '$locale',
    '$location',
    'DatasetResource',
    'DatasetPublicationResource',
    'DraftDatasetStatusResource',
    'DraftDatasetViewRevisionResource',
    'DraftDatasetRevisionsResource',
    'DraftDatasetRestoreRevisionResource',
    'DraftFileSystemSearchResource',
    'MicaConfigResource',
    'ActiveTabService',
    'NOTIFICATION_EVENTS',
    '$filter',
    'DatasetService',
    'DocumentPermissionsService',

    function ($rootScope,
              $scope,
              $routeParams,
              $log,
              $locale,
              $location,
              DatasetResource,
              DatasetPublicationResource,
              DraftDatasetStatusResource,
              DraftDatasetViewRevisionResource,
              DraftDatasetRevisionsResource,
              DraftDatasetRestoreRevisionResource,
              DraftFileSystemSearchResource,
              MicaConfigResource,
              ActiveTabService,
              NOTIFICATION_EVENTS,
              $filter,
              DatasetService,
              DocumentPermissionsService) {
      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({lang: lang});
        });
        $scope.openAccess = micaConfig.openAccess;
      });

      var initializeDataset = function(dataset) {
        $scope.permissions = DocumentPermissionsService.state(dataset['obiba.mica.EntityStateDto.datasetState']);
      };

      var getTypeFromUrl = function() {
        var matched = /\/(\w+-dataset)\//.exec($location.path());
        return matched ? matched[1] : '';
      };

      $scope.Mode = {View: 0, Revision: 1, File: 2, Permission: 3, Comment: 4};
      $scope.type = getTypeFromUrl();
      $scope.datasetId = $routeParams.id;
      $scope.getActiveTab = ActiveTabService.getActiveTab;
      $scope.dataset = DatasetResource.get({id: $routeParams.id, type: $scope.type}, initializeDataset);

      $scope.publish = function (publish) {
        if (publish) {
          DraftFileSystemSearchResource.searchUnderReview({path: '/' + $scope.type + '/' + $scope.dataset.id},
            function onSuccess(response) {
              DatasetPublicationResource.publish(
                {id: $scope.dataset.id, type: $scope.type, cascading: response.length > 0 ? 'UNDER_REVIEW' : 'NONE'},
                function () {
                  $scope.dataset = DatasetResource.get({id: $routeParams.id, type: $scope.type}, initializeDataset);
                });
            },
            function onError() {
              $log.error('Failed to search for Under Review files.');
            }
          );
        } else {
          DatasetPublicationResource.unPublish({id: $scope.dataset.id, type: $scope.type}, function () {
            $scope.dataset = DatasetResource.get({id: $routeParams.id, type: $scope.type}, initializeDataset);
          });
        }
      };

      $scope.toStatus = function (value) {
        DraftDatasetStatusResource.toStatus({id: $scope.dataset.id, type:$scope.type, value: value}, function () {
          $scope.dataset = DatasetResource.get({id: $routeParams.id, type: $scope.type}, initializeDataset);
        });
      };

      $scope.delete = function () {
        $scope.dataset.type = $scope.type;
        DatasetService.deleteDataset($scope.dataset, function () {
          $location.path('/' + $scope.type);
        });
      };

      var getViewMode = function() {
        var result = /\/(revision[s\/]*|files|permissions)/.exec($location.path());
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

      $scope.viewMode = getViewMode();

      var viewRevision = function (datasetId, commitInfo) {
        $scope.commitInfo = commitInfo;
        $scope.dataset = DraftDatasetViewRevisionResource.view({
          id: datasetId,
          commitId: commitInfo.commitId,
          type: $scope.type
        });
      };

      var fetchDataset = function (datasetId) {
        $scope.dataset = DatasetResource.get({id: datasetId, type: $scope.type}, initializeDataset);
      };

      var fetchRevisions = function (datasetId, onSuccess) {
        DraftDatasetRevisionsResource.query({id: datasetId, type: $scope.type}, function (response) {
          if (onSuccess) {
            onSuccess(response);
          }
        });
      };

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

      $scope.fetchDataset = fetchDataset;
      $scope.viewRevision = viewRevision;
      $scope.restoreRevision = restoreRevision;
      $scope.fetchRevisions = fetchRevisions;

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

  .controller('HarmonizationDatasetListController', ['$rootScope', '$scope', 'HarmonizationDatasetsResource', 'DatasetService',
    function ($rootScope, $scope, HarmonizationDatasetsResource, DatasetService) {
      var onSuccess = function(response) {
        $scope.harmonizedDatasets = response;
        $scope.loading = false;
      };

      var onError = function() {
        $scope.loading = false;
      };

      $scope.loading = true;
      HarmonizationDatasetsResource.query({}, onSuccess, onError);

      $scope.deleteHarmonizationDataset = function (dataset) {
        DatasetService.deleteDataset(dataset, function () {
          $scope.loading = true;
          HarmonizationDatasetsResource.query({}, onSuccess, onError);
        });
      };
    }])

  .controller('HarmonizationDatasetViewController', ['$rootScope',
    '$scope',
    '$routeParams',
    '$log',
    '$locale',
    '$location',
    'HarmonizationDatasetResource',
    'HarmonizationDatasetPublicationResource',
    'MicaConfigResource',
    'ActiveTabService',

    function ($rootScope,
              $scope,
              $routeParams,
              $log,
              $locale,
              $location,
              HarmonizationDatasetResource,
              HarmonizationDatasetPublicationResource,
              MicaConfigResource,
              ActiveTabService) {

      MicaConfigResource.get(function (micaConfig) {
        $scope.opal = micaConfig.opal;
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({lang: lang});
        });
      });

      $scope.dataset = HarmonizationDatasetResource.get({id: $routeParams.id}, function (dataset) {
        $scope.datasetProject = dataset['obiba.mica.HarmonizationDatasetDto.type'].project;
        $scope.datasetTable = dataset['obiba.mica.HarmonizationDatasetDto.type'].table;
      });

      $scope.getActiveTab = ActiveTabService.getActiveTab;

      $scope.isPublished = function () {
        return $scope.dataset.published;
      };

      $scope.publish = function () {
        if ($scope.dataset.published) {
          HarmonizationDatasetPublicationResource.unPublish({id: $scope.dataset.id}, function () {
            $scope.dataset = HarmonizationDatasetResource.get({id: $routeParams.id});
          });
        } else {
          HarmonizationDatasetPublicationResource.publish({id: $scope.dataset.id}, function () {
            $scope.dataset = HarmonizationDatasetResource.get({id: $routeParams.id});
          });
        }
      };

    }])

  .controller('StudyTableModalController', ['$scope', '$modalInstance', '$log', 'MicaConfigResource', 'StudyStatesResource', 'StudyStateProjectsResource', 'studyTable', 'tab',
    function ($scope, $modalInstance, $log, MicaConfigResource, StudyStatesResource, StudyStateProjectsResource, studyTable, tab) {
      $scope.studies = [];
      $scope.projects = [];
      $scope.selected = {};
      $scope.studyTable = $.extend(true, {}, studyTable);
      $scope.tab = tab;

      if (studyTable && studyTable !== {}) {
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

      $scope.$watch('selected.study', function () {
        if ($scope.selected.study && $scope.selected.study.id) {
          StudyStateProjectsResource.query({id: $scope.selected.study.id}).$promise.then(function (projects) {
            $scope.projects = projects;
            var selectedTable, selectedProject = $scope.projects.filter(function (p) {return p.name === studyTable.project; })[0];

            if (selectedProject) {
              $scope.selected.project = selectedProject;

              selectedTable = selectedProject.datasource.table.filter(function (t) {return t === studyTable.table; })[0];

              if (selectedTable) {
                $scope.selected.project.table = selectedTable;
              }
            }

          });
        }
      });

      $scope.save = function (form) {
        if (form.$valid) {
          angular.extend($scope.studyTable, {
            studyId: $scope.selected.study.id,
            populationId: $scope.selected.study.population.id,
            dataCollectionEventId: $scope.selected.study.population.dataCollectionEvent.id,
            project: $scope.selected.project.name,
            table: $scope.selected.project.table
          });

          $modalInstance.close($scope.studyTable);
          return;
        }

        $scope.form = form;
        $scope.form.saveAttempted = true;
      };

      $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
      };

    }]);
