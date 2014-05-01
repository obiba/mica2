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
  .controller('StudyViewController', ['$scope', '$routeParams', '$log', '$locale', 'StudyResource', 'MicaConfigResource',

    function ($scope, $routeParams, $log, $locale, StudyResource, MicaConfigResource) {

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({ lang: lang });
        });
      });

      $scope.study = StudyResource.get({id: $routeParams.id});

      $scope.months = $locale.DATETIME_FORMATS.MONTH;

      $log.debug('months', $scope.months);
      $log.debug('study', $scope.study);

    }])

  .controller('StudyEditController', ['$scope', '$routeParams', '$log', 'StudyResource', 'MicaConfigResource',

    function ($scope, $routeParams, $log, StudyResource, MicaConfigResource) {

      $scope.study = StudyResource.get({id: $routeParams.id});
      $log.debug('study', $scope.study);

      MicaConfigResource.get(function (micaConfig) {
        $scope.languages = micaConfig.languages;
        $log.debug('languages', $scope.languages);
      });

    }]);
