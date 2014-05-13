'use strict';

mica.contact
  .controller('ContactController', ['$scope', '$modal', '$log',
    function ($scope, $modal, $log) {

      $scope.viewContact = function (contact) {
        $log.debug('viewContact', contact);
        $scope.contact = contact;
        $modal.open({
          templateUrl: 'app/contact/contact-modal-view.html',
          controller: 'ContactViewModalController',
          resolve: {
            contact: function () {
              return $scope.contact;
            }
          }
        });
      };

      $scope.editContact = function (contact) {
        $log.debug('editContact', contact);
        $scope.contact = contact;
        $modal
          .open({
            templateUrl: 'app/contact/contact-modal-form.html',
            controller: 'ContactEditModalController',
            resolve: {
              contact: function () {
                return $scope.contact;
              }
            }
          })
          .result.then(function (contact) {
            $scope.contact = contact;
          });
      }

    }])
  .controller('ContactViewModalController', ['$scope', '$modalInstance', '$log', 'MicaConfigResource', 'contact',
    function ($scope, $modalInstance, $log, MicaConfigResource, contact) {

      $log.debug('view contact', contact);

      $scope.contact = contact;

      MicaConfigResource.get(function (micaConfig) {
        $scope.languages = micaConfig.languages;
      });

      $scope.close = function () {
        $modalInstance.dismiss('close');
      };

    }])
  .controller('ContactEditModalController', ['$scope', '$modalInstance', '$log', 'MicaConfigResource', 'contact',
    function ($scope, $modalInstance, $log, MicaConfigResource, contact) {

      $log.debug('edit contact', contact);

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

