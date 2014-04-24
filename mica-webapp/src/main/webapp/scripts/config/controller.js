'use strict';

micaApp.controller('MicaConfigController', ['$scope', '$resource', 'resolvedMicaConfig', 'MicaConfig',
  function ($scope, $resource, resolvedMicaConfig, MicaConfig) {

    $scope.micaConfig = resolvedMicaConfig;

    $scope.save = function () {
      MicaConfig.save($scope.micaConfig,
        function () {
          $scope.micaConfig = MicaConfig.get();
          $('#micaConfigModal').modal('hide');
          $scope.micaConfig = MicaConfig.get();
        });
    };

    $scope.edit = function (id) {
      $scope.micaConfig = MicaConfig.get();
      $scope.availableLanguages = $resource('ws/config/languages').get();
      $('#micaConfigModal').modal('show');
    };

  }]);
