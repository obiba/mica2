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

    }]);
