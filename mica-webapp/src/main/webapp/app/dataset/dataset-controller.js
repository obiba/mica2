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

  .controller('StudyDatasetEditController', ['$rootScope', '$scope', '$routeParams', '$log', '$locale', '$location', 'StudyDatasetResource', 'DraftStudyDatasetsResource', 'StudyDatasetPublicationResource', 'MicaConfigResource',

    function ($rootScope, $scope, $routeParams, $log, $locale, $location, StudyDatasetResource, DraftStudyDatasetsResource, StudyDatasetPublicationResource, MicaConfigResource) {

      $scope.dataset = $routeParams.id ?
        StudyDatasetResource.get({id: $routeParams.id},
          function (dataset) {
            dataset.studyTable = dataset['obiba.mica.StudyDatasetDto.type'].studyTable
          }) :
        {published: false, 'obiba.mica.StudyDatasetDto.type': {} };

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
        $log.debug('Update study dataset', $scope.dataset['obiba.mica.StudyDatasetDto.type']);
        $scope.dataset.studyTable = $scope.dataset['obiba.mica.StudyDatasetDto.type'].studyTable;
        $scope.dataset.$save(
          function (dataset) {
            $location.path('/study-dataset/' + dataset.id).replace();
          },
          saveErrorHandler);
      };

      var createDataset = function () {
        $log.debug('Create new study dataset', $scope.studyTable);
        // HACK until we find a way to set DTO extension field through models, currently we cannot add
        // fields to a obj['dot.separated.name'].field, AngularJS parser discards obj['dot.separated.name']
        $scope.dataset['obiba.mica.StudyDatasetDto.type'].studyTable = $scope.dataset.studyTable;
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

  .controller('HarmonizedDatasetListController', ['$scope', 'HarmonizedDatasetsResource', 'HarmonizedDatasetResource',

    function ($scope, HarmonizedDatasetsResource, HarmonizedDatasetResource) {

      $scope.harmonizedDatasets = HarmonizedDatasetsResource.query();

      $scope.deleteHarmonizedDataset = function (id) {
        //TODO ask confirmation
        HarmonizedDatasetResource.delete({id: id},
          function () {
            $scope.studyDatasets = HarmonizedDatasetsResource.query();
          });
      };

    }])

  .controller('HarmonizedDatasetViewController', ['$rootScope', '$scope', '$routeParams', '$log', '$locale', '$location', 'HarmonizedDatasetResource', 'HarmonizedDatasetPublicationResource', 'MicaConfigResource',

    function ($rootScope, $scope, $routeParams, $log, $locale, $location, HarmonizedDatasetResource, HarmonizedDatasetPublicationResource, MicaConfigResource) {

      MicaConfigResource.get(function (micaConfig) {
        $scope.opal = micaConfig.opal;
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({lang: lang});
        });
      });

      $scope.dataset = HarmonizedDatasetResource.get({id: $routeParams.id}, function (dataset) {
        $scope.datasetProject = dataset['obiba.mica.HarmonizedDatasetDto.type'].project;
        $scope.datasetTable = dataset['obiba.mica.HarmonizedDatasetDto.type'].table;
      });

      $scope.isPublished = function() {
        return $scope.dataset.published;
      };

      $scope.publish = function () {
        if ($scope.dataset.published) {
          HarmonizedDatasetPublicationResource.unPublish({id: $scope.dataset.id}, function () {
            $scope.dataset = HarmonizedDatasetResource.get({id: $routeParams.id});
          });
        } else {
          HarmonizedDatasetPublicationResource.publish({id: $scope.dataset.id}, function () {
            $scope.dataset = HarmonizedDatasetResource.get({id: $routeParams.id});
          });
        }
      };

    }]);
