'use strict';

micaApp.controller('StudyListController', ['$scope', 'StudiesResource', 'StudyResource',

  function ($scope, StudiesResource, StudyResource) {

    $scope.studies = StudiesResource.query();

    $scope.deleteStudy = function (id) {
      StudyResource.delete({id: id},
        function () {
          $scope.studies = StudiesResource.query();
        });
    };

  }])
  .controller('StudyViewController', ['$scope', '$routeParams', 'StudyResource',

    function ($scope, $routeParams, StudyResource) {

      $scope.study = StudyResource.get({id: $routeParams.id});

    }])

  .controller('StudyEditController', ['$scope', '$routeParams', 'StudyResource',

    function ($scope, $routeParams, StudyResource) {

      $scope.study = StudyResource.get({id: $routeParams.id});

    }]);
