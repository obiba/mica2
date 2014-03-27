'use strict';

micaApp.controller('StudyController', ['$scope', 'resolvedStudy', 'Study',
  function ($scope, resolvedStudy, Study) {

    $scope.studys = resolvedStudy;

    $scope.create = function () {
      Study.save($scope.study,
        function () {
          $scope.studys = Study.query();
          $('#saveStudyModal').modal('hide');
          $scope.clear();
        });
    };

    $scope.update = function (id) {
      $scope.study = Study.get({id: id});
      $('#saveStudyModal').modal('show');
    };

    $scope.delete = function (id) {
      Study.delete({id: id},
        function () {
          $scope.studys = Study.query();
        });
    };

    $scope.clear = function () {
      $scope.study = {id: "", sampleTextAttribute: "", sampleDateAttribute: ""};
    };
  }]);
