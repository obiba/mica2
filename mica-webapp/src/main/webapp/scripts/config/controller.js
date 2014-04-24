'use strict';

micaApp.controller('MicaConfigController', ['$scope', '$resource', '$modal', 'resolvedMicaConfig', 'MicaConfig',
  function ($scope, $resource, $modal, resolvedMicaConfig, MicaConfig) {

    $scope.micaConfig = resolvedMicaConfig;
    $scope.availableLanguages = $resource('ws/config/languages').get();

    $scope.edit = function (id) {
      $modal.open({
        templateUrl: 'views/config/form.html',
        controller: MicaConfigModalController,
        resolve: {
          micaConfig: function () {
            return MicaConfig.get();
          },
          availableLanguages: function () {
            return $scope.availableLanguages;
          }
        }
      }).result.then(function () {
          $scope.micaConfig = MicaConfig.get();
        });
    };

  }]);

var MicaConfigModalController = function ($scope, $modalInstance, MicaConfig, micaConfig, availableLanguages) {

  $scope.micaConfig = micaConfig;
  $scope.availableLanguages = availableLanguages;

  $scope.save = function () {
    MicaConfig.save($scope.micaConfig,
      function () {
        $modalInstance.close();
      });
  };

  $scope.cancel = function () {
    $modalInstance.dismiss('cancel');
  };

};
