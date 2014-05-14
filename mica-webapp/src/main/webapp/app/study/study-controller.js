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
  .controller('StudyViewController', ['$rootScope', '$scope', '$routeParams', '$log', '$locale', '$location', 'DraftStudyResource', 'MicaConfigResource',

    function ($rootScope, $scope, $routeParams, $log, $locale, $location, DraftStudyResource, MicaConfigResource) {

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

      $scope.$on('studyUpdatedEvent', function (event, studyUpdated) {
        if (studyUpdated == $scope.study) {
          $log.debug('save study', studyUpdated);

          $scope.study.$save(function () {
              $scope.study = DraftStudyResource.get({id: $scope.study.id});
            },
            function (response) {
              $log.error('Error on study save:', response);
              $rootScope.$broadcast('showNotificationDialogEvent', {
                //TODO i18n
                "iconClass": "fa-exclamation-triangle",
                "title": "Error while saving study",
                "message": response.data ? response.data : angular.fromJson(response)
              });
            });
        }
      });

      $scope.sortableOptions = {
        stop: function (e, ui) {
          $scope.$emit('studyUpdatedEvent', $scope.study);
        }
      };

      $scope.$on('addInvestigatorEvent', function (event, study, contact) {
        if (study == $scope.study) {
          if (!$scope.study.investigators) $scope.study.investigators = [];
          $scope.study.investigators.push(contact);
          $scope.$emit('studyUpdatedEvent', $scope.study);
        }
      });

      $scope.$on('addContactEvent', function (event, study, contact) {
        if (study == $scope.study) {
          if (!$scope.study.contacts) $scope.study.contacts = [];
          $scope.study.contacts.push(contact);
          $scope.$emit('studyUpdatedEvent', $scope.study);
        }
      });

      $scope.$on('contactUpdatedEvent', function (event, study, contact) {
        if (study == $scope.study) {
          $scope.$emit('studyUpdatedEvent', $scope.study);
        }
      });

      $scope.$on('contactEditionCanceledEvent', function (event, study) {
        if (study == $scope.study) {
          $scope.study = DraftStudyResource.get({id: $scope.study.id});
        }
      });

      $scope.$on('contactDeletedEvent', function (event, study, contact, isInvestigator) {
        if (study == $scope.study) {
          if (isInvestigator) {
            var investigatorsIndex = $scope.study.investigators.indexOf(contact);
            if (investigatorsIndex != -1) $scope.study.investigators.splice(investigatorsIndex, 1);
          } else {
            var contactsIndex = $scope.study.contacts.indexOf(contact);
            if (contactsIndex != -1) $scope.study.contacts.splice(contactsIndex, 1);
          }
          $scope.$emit('studyUpdatedEvent', $scope.study);
        }
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
