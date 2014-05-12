'use strict';

mica.study
  .controller('StudyListController', ['$scope', 'DraftStudiesResource', 'DraftStudyResource',

    function ($scope, DraftStudiesResource, DraftStudyResource) {

      $scope.studies = DraftStudiesResource.query();

      $scope.deleteStudy = function (id) {
        DraftStudyResource.delete({id: id},
          function () {
            $scope.studies = DraftStudiesResource.query();
          });
      };

    }])
  .controller('StudyViewController', ['$scope', '$routeParams', '$log', '$locale', 'DraftStudyResource', 'MicaConfigResource',

    function ($scope, $routeParams, $log, $locale, DraftStudyResource, MicaConfigResource) {

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({ lang: lang });
        });
      });

      $scope.study = DraftStudyResource.get({id: $routeParams.id});
      $scope.study.$promise.then(function (studyDto) {
        new $.MicaTimeline(new $.StudyDtoParser()).create("#timeline", studyDto);
      });

      $scope.months = $locale.DATETIME_FORMATS.MONTH;
      $log.debug('months', $scope.months);
      $log.debug('study', $scope.study);
    }])

  .controller('StudyEditController', ['$scope', '$routeParams', '$log', 'DraftStudyResource', 'MicaConfigResource',

    function ($scope, $routeParams, $log, DraftStudyResource, MicaConfigResource) {

      $scope.study = DraftStudyResource.get({id: $routeParams.id});
      $log.debug('study', $scope.study);

      MicaConfigResource.get(function (micaConfig) {
        $scope.languages = micaConfig.languages;
        $log.debug('languages', $scope.languages);
      });


      $scope.save = function () {
        $log.debug('scope.form', $scope.form);
      };

    }]);
