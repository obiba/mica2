'use strict';

mica.dataset

  .controller('StudyDatasetListController', ['$scope', 'StudyDatasetsResource', 'StudyDatasetResource',

    function ($scope, StudyDatasetsResource, StudyDatasetResource) {

      $scope.studyDatasets = StudyDatasetsResource.query();

      $scope.deleteStudyDataset = function (id) {
        //TODO ask confirmation
        StudyDatasetResource.delete({id: id},
          function () {
            $scope.studyDatasets = StudyDatasetsResource.query();
          });
      };

    }])

  .controller('StudyDatasetEditController', ['$rootScope', '$scope', '$routeParams', '$log', '$locale', '$location', 'StudyDatasetResource', 'DraftStudyDatasetsResource', 'StudyDatasetPublicationResource', 'MicaConfigResource', 'FormServerValidation',

    function ($rootScope, $scope, $routeParams, $log, $locale, $location, StudyDatasetResource, DraftStudyDatasetsResource, StudyDatasetPublicationResource, MicaConfigResource, FormServerValidation) {

      $scope.dataset = $routeParams.id ?
        StudyDatasetResource.get({id: $routeParams.id}) : {published: false, 'obiba.mica.StudyDatasetDto.type': {} };

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
          updateStudy();
        } else {
          createDataset();
        }
      };

      $scope.cancel = function () {
        $location.path('/study-dataset' + ($scope.dataset.id ? '/' + $scope.dataset.id : '')).replace();
      };

      var updateStudy = function () {
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


  .controller('HarmonizationDatasetEditController', ['$rootScope', '$scope', '$routeParams', '$log', '$locale', '$location', '$modal', 'HarmonizationDatasetResource', 'DraftHarmonizationDatasetsResource', 'HarmonizationDatasetPublicationResource', 'MicaConfigResource', 'FormServerValidation',

    function ($rootScope, $scope, $routeParams, $log, $locale, $location, $modal, HarmonizationDatasetResource, DraftHarmonizationDatasetsResource, HarmonizationDatasetPublicationResource, MicaConfigResource, FormServerValidation) {

      $scope.dataset = $routeParams.id ? HarmonizationDatasetResource.get({id: $routeParams.id}) : {published: false, 'obiba.mica.HarmonizationDatasetDto.type': {} };

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

      $scope.addStudyTable = function() {
        $modal
          .open({
            templateUrl: 'app/dataset/views/study-table-modal-form.html',
            controller: 'StudyTableModalController',
            resolve: {
              studyTable: function () {
                return {};
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

      $scope.editStudyTable = function(index) {
        $modal
          .open({
            templateUrl: 'app/dataset/views/study-table-modal-form.html',
            controller: 'StudyTableModalController',
            resolve: {
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

      $scope.deleteStudyTable = function(index) {
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

  .controller('StudyDatasetViewController', ['$rootScope', '$scope', '$routeParams', '$log', '$locale', '$location', 'StudyDatasetResource', 'StudyDatasetPublicationResource', 'MicaConfigResource',

    function ($rootScope, $scope, $routeParams, $log, $locale, $location, StudyDatasetResource, StudyDatasetPublicationResource, MicaConfigResource) {

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({lang: lang});
        });
      });

      $scope.dataset = StudyDatasetResource.get({id: $routeParams.id});

      $scope.isPublished = function() {
        return $scope.dataset.published;
      };

      $scope.publish = function () {
        if ($scope.dataset.published) {
          StudyDatasetPublicationResource.unPublish({id: $scope.dataset.id}, function () {
            $scope.dataset = StudyDatasetResource.get({id: $routeParams.id});
          });
        } else {
          StudyDatasetPublicationResource.publish({id: $scope.dataset.id}, function () {
            $scope.dataset = StudyDatasetResource.get({id: $routeParams.id});
          });
        }
      };

    }])

  .controller('HarmonizationDatasetListController', ['$scope', 'HarmonizationDatasetsResource', 'HarmonizationDatasetResource',

    function ($scope, HarmonizationDatasetsResource, HarmonizationDatasetResource) {

      $scope.harmonizedDatasets = HarmonizationDatasetsResource.query();

      $scope.deleteHarmonizedDataset = function (id) {
        //TODO ask confirmation
        HarmonizationDatasetResource.delete({id: id},
          function () {
            $scope.studyDatasets = HarmonizationDatasetsResource.query();
          });
      };

    }])

  .controller('HarmonizationDatasetViewController', ['$rootScope', '$scope', '$routeParams', '$log', '$locale', '$location', 'HarmonizationDatasetResource', 'HarmonizationDatasetPublicationResource', 'MicaConfigResource',

    function ($rootScope, $scope, $routeParams, $log, $locale, $location, HarmonizationDatasetResource, HarmonizationDatasetPublicationResource, MicaConfigResource) {

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

      $scope.isPublished = function() {
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

  .controller('StudyTableModalController', ['$scope', '$modalInstance', '$log', 'MicaConfigResource', 'studyTable',
    function ($scope, $modalInstance, $log, MicaConfigResource, studyTable) {
      $scope.studyTable =  $.extend(true, {}, studyTable);

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({ lang: lang, labelKey: 'language.' + lang });
        });
      });

      $scope.save = function (form) {
        if (form.$valid) {
          $modalInstance.close($scope.studyTable);
        }
        else {
          $scope.form = form;
          $scope.form.saveAttempted = true;
        }
      };

      $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
      };

    }]);
