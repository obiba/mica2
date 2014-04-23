'use strict';

micaApp.controller('MicaConfigController', ['$scope', 'resolvedMicaConfig', 'MicaConfig',
  function ($scope, resolvedMicaConfig, MicaConfig) {

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
      $('#micaConfigModal').modal('show');
    };

  }]);
