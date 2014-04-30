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
  .controller('StudyViewController', ['$scope', '$routeParams', '$log', 'StudyResource', 'MicaConfigResource',

    function ($scope, $routeParams, $log, StudyResource, MicaConfigResource) {

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({ lang: lang });
        });
      });

      $scope.study = StudyResource.get({id: $routeParams.id});

      $log.debug('study', $scope.study);

    }])

  .controller('StudyEditController', ['$scope', '$routeParams', 'StudyResource',

    function ($scope, $routeParams, StudyResource) {

      $scope.study = StudyResource.get({id: $routeParams.id});

    }]);
