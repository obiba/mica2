'use strict';

micaApp.controller('MicaConfigController', ['$scope', 'resolvedMicaConfig', 'MicaConfig',
  function ($scope, resolvedMicaConfig, MicaConfig) {

    $scope.micaConfig = resolvedMicaConfig;

    $scope.save = function () {
      MicaConfig.save($scope.micaConfig,
        function () {
          $scope.micaConfig = MicaConfig.get();
          $('#micaConfigModal').modal('hide');
          $scope.clear();
        });
    };

    $scope.edit = function (id) {
      $scope.micaConfig = MicaConfig.get();
      $('#micaConfigModal').modal('show');
    };

    $scope.clear = function () {
      $scope.micaConfig = {name: "", defaultCharacterSet: "", publicUrl: "", locales: []};
    };
  }]);
