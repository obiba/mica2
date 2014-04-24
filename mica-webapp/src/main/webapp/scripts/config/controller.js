'use strict';

micaApp.controller('MicaConfigController', ['$scope', 'resolvedMicaConfig', 'MicaConfig', 'MicaConfigLanguages',
  function ($scope, resolvedMicaConfig, MicaConfig, MicaConfigLanguages) {

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
      $scope.availableLanguages = MicaConfigLanguages.get();
      $('#micaConfigModal').modal('show');
    };

  }]);
