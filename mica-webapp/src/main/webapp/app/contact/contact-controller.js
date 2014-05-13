'use strict';

mica.contact.controller('ContactViewController', ['$scope', '$resource', '$modal', '$log', 'contact', 'MicaConfigResource',
  function ($scope, $resource, $modal, $log, contact, MicaConfigResource) {

    $log.debug('view contact', contact);

    $scope.contact = contact;

    $scope.edit = function () {

      $modal
        .open({
          templateUrl: 'app/contact/contact-modal-form.html',
          controller: ContactEditController,
          resolve: {
            contact: function () {
              return $scope.contact;
            },
            MicaConfigResource: MicaConfigResource
          }
        })
        .result.then(function (contact) {
          $scope.contact = contact;
        });
    };

  }]);

var ContactEditController = function ($scope, $modalInstance, contact, MicaConfigResource) {

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

};
