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
  .controller('StudyViewController', ['$scope', '$routeParams', '$log', '$locale', '$location', 'DraftStudyResource', 'MicaConfigResource',

    function ($scope, $routeParams, $log, $locale, $location, DraftStudyResource, MicaConfigResource) {

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({ lang: lang });
        });
      });

      $scope.study = DraftStudyResource.get(
        {id: $routeParams.id},
        function (study) {
          new $.MicaTimeline(new $.StudyDtoParser()).create("#timeline", study);
        });

      $scope.months = $locale.DATETIME_FORMATS.MONTH;
//      $log.debug('months', $scope.months);
//      $log.debug('study', $scope.study);


      $scope.$on('contactUpdated', function (event, contact) {
        $log.debug('contactUpdated', contact);
        $scope.study.$save(function (study) {
            $scope.study = DraftStudyResource.get({id: $scope.study.id});
          },
          function (response) {
            $log.error('error response:', response);
            //TODO show error message with bootstrap modal
            alert(response);
          });
      });

      $scope.$on('contactEditionCanceled', function (event) {
        $scope.study = DraftStudyResource.get({id: $scope.study.id});
      });

    }])

  .controller('StudyEditController', ['$scope', '$routeParams', '$log', '$location', 'DraftStudyResource', 'MicaConfigResource',

    function ($scope, $routeParams, $log, $location, DraftStudyResource, MicaConfigResource) {

      $scope.study = DraftStudyResource.get({id: $routeParams.id});
      $log.debug('study', $scope.study);

      MicaConfigResource.get(function (micaConfig) {
        $scope.languages = micaConfig.languages;
      });

      $scope.save = function () {

        if ($scope.study.id) {

          $log.debug('Update study', $scope.study);
          $scope.study.$save(function (study) {
              $location.path('/study/' + study.id).replace();
            },
            function (response) {
              $log.debug('error response:', response);
              $scope.errors = [];
              response.data.forEach(function (error) {
                //$log.debug('error: ', error);
                var field = error.path.substring(error.path.indexOf('.') + 1);
                $scope.form[field].$dirty = true;
                $scope.form[field].$setValidity('server', false);
                $scope.errors[field] = error.message;
              });
            });

        } else {
          $log.debug('Create new study', $scope.study);
        }

      };

    }]);
