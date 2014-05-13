'use strict';

mica.contact
  .controller('ContactController', ['$scope', '$modal', '$log',
    function ($scope, $modal, $log) {

      $scope.viewContact = function (contact) {
        $modal.open({
          templateUrl: 'app/contact/contact-modal-view.html',
          controller: 'ContactViewModalController',
          resolve: {
            contact: function () {
              return contact;
            }
          }
        });
      };

      $scope.editContact = function (contact) {
        $modal
          .open({
            templateUrl: 'app/contact/contact-modal-form.html',
            controller: 'ContactEditModalController',
            resolve: {
              contact: function () {
                return contact;
              }
            }
          })
          .result.then(function (contact) {
            $scope.$emit('contactUpdated', contact);
          }, function () {
            $scope.$emit('contactEditionCanceled');
          });
      };

      $scope.addInvestigator = function (study) {
        $scope.addInvestigatorOrContact(study, true);
      };

      $scope.addContact = function (study) {
        $scope.addInvestigatorOrContact(study, false);
      };

      $scope.addInvestigatorOrContact = function (study, isInvestigator) {
        $modal
          .open({
            templateUrl: 'app/contact/contact-modal-form.html',
            controller: 'ContactEditModalController',
            resolve: {
              contact: function () {
                return {};
              }
            }
          })
          .result.then(function (contact) {
            if (isInvestigator) {
              study.investigators.push(contact);
            } else {
              study.contacts.push(contact);
            }
            $scope.$emit('contactUpdated', contact);
          }, function () {
            $scope.$emit('contactEditionCanceled');
          });
      };

    }])
  .controller('ContactViewModalController', ['$scope', '$modalInstance', '$log', 'MicaConfigResource', 'contact',
    function ($scope, $modalInstance, $log, MicaConfigResource, contact) {

      $scope.contact = contact;

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({ lang: lang });
        });
      });

      $scope.close = function () {
        $modalInstance.dismiss('close');
      };

    }])
  .controller('ContactEditModalController', ['$scope', '$modalInstance', '$log', 'MicaConfigResource', 'contact',
    function ($scope, $modalInstance, $log, MicaConfigResource, contact) {

      $scope.contact = contact;

      MicaConfigResource.get(function (micaConfig) {
        $scope.languages = micaConfig.languages;
      });

      $scope.save = function () {
        $modalInstance.close($scope.contact);
      };

      $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
      };

    }]);

