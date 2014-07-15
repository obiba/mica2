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

  .controller('StudyDatasetViewController', ['$rootScope', '$scope', '$routeParams', '$log', '$locale', '$location', 'StudyDatasetResource', 'StudyDatasetPublicationResource', 'StudyDatasetIndexResource', 'MicaConfigResource',

    function ($rootScope, $scope, $routeParams, $log, $locale, $location, StudyDatasetResource, StudyDatasetPublicationResource, StudyDatasetIndexResource, MicaConfigResource) {

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({lang: lang});
        });
      });

      $scope.dataset = StudyDatasetResource.get({id: $routeParams.id});

      $scope.publish = function () {
        StudyDatasetPublicationResource.publish({id: $scope.dataset.id}, function () {
          $scope.dataset = StudyDatasetResource.get({id: $routeParams.id});
        });
      };

      $scope.unPublish = function () {
        StudyDatasetPublicationResource.unPublish({id: $scope.dataset.id}, function () {
          $scope.dataset = StudyDatasetResource.get({id: $routeParams.id});
        });
      };

      $scope.index = function () {
        StudyDatasetIndexResource.index({id: $scope.dataset.id}, function () {});
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

  .controller('HarmonizedDatasetViewController', ['$rootScope', '$scope', '$routeParams', '$log', '$locale', '$location', 'HarmonizedDatasetResource', 'HarmonizedDatasetPublicationResource', 'HarmonizedDatasetIndexResource', 'MicaConfigResource',

    function ($rootScope, $scope, $routeParams, $log, $locale, $location, HarmonizedDatasetResource, HarmonizedDatasetPublicationResource, HarmonizedDatasetIndexResource, MicaConfigResource) {

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

      $scope.publish = function () {
        HarmonizedDatasetPublicationResource.publish({id: $scope.dataset.id}, function () {
          $scope.dataset = HarmonizedDatasetResource.get({id: $routeParams.id});
        });
      };

      $scope.unPublish = function () {
        HarmonizedDatasetPublicationResource.unPublish({id: $scope.dataset.id}, function () {
          $scope.dataset = HarmonizedDatasetResource.get({id: $routeParams.id});
        });
      };

      $scope.index = function () {
        HarmonizedDatasetIndexResource.index({id: $scope.dataset.id}, function () {});
      };

    }]);
