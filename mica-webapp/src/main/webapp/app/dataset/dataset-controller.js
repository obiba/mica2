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
