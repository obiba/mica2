'use strict';

mica.config
  .controller('MicaConfigController', ['$scope', '$resource', '$route', '$window', '$log', 'MicaConfigResource', '$modal',
    'OpalCredentialsResource', 'OpalCredentialResource', 'KeyStoreResource',

    function ($scope, $resource, $route, $window, $log, MicaConfigResource, $modal,
              OpalCredentialsResource, OpalCredentialResource, KeyStoreResource) {
      $scope.micaConfig = MicaConfigResource.get();
      $scope.availableLanguages = $resource('ws/config/languages').get();
      $scope.opalCredentials = OpalCredentialsResource.query();

      $scope.status = {
        isopen: true
      };

      $scope.toggled = function(open) {
        $log.log('Dropdown is now: ', open);
      };

      $scope.toggleDropdown = function($event) {
        $event.preventDefault();
        $event.stopPropagation();
        $scope.status.isopen = !$scope.status.isopen;
      };

      $scope.downloadCertificate = function () {
        $window.open('ws/config/keystore/system/https', '_blank', '');
      };

      $scope.createKeyPair = function () {
        $modal.open({
          templateUrl: 'app/config/views/config-modal-create-keypair.html',
          controller: 'CreateKeyPairModalController',
          resolve : {
            isOpalCredential : function() {
              return false;
            }
          }
        }).result.then(function(data) {
            KeyStoreResource.save(data).$promise.then(function () {
              $route.reload();
            });
        });
      };

      $scope.importKeyPair = function () {
        $modal.open({
          templateUrl: 'app/config/views/config-modal-import-keypair.html',
          controller: 'ImportKeyPairModalController',
          resolve : {
            isOpalCredential : function() {
              return false;
            }
          }
        }).result.then(function(data) {
            KeyStoreResource.save(data).$promise.then(function () {
              $route.reload();
            });
          });
      };

      $scope.createOpalCredentialKeyPair = function () {
        $modal.open({
          templateUrl: 'app/config/views/config-modal-create-keypair.html',
          controller: 'CreateKeyPairModalController',
          resolve: {
            isOpalCredential: function () {
              return true;
            }
          }
        }).result.then(function (data) {
            OpalCredentialsResource.save(data).$promise.then(function () {
              $route.reload();
            });
          });
      };

      $scope.importOpalCredentialKeyPair = function () {
        $modal.open({
          templateUrl: 'app/config/views/config-modal-import-keypair.html',
          controller: 'ImportKeyPairModalController',
          resolve: {
            isOpalCredential: function () {
              return true;
            }
          }
        }).result.then(function (data) {
            OpalCredentialsResource.save(data).$promise.then(function () {
              $route.reload();
            });
          });
      };

      $scope.addOpalCredentialUserPass = function () {
        $modal.open({
          templateUrl: 'app/config/views/config-modal-username-credential.html',
          controller: 'UsernamePasswordModalController',
          resolve: {
            opalCredential: function () {
              return null;
            }
          }
        }).result.then(function (data) {
            OpalCredentialsResource.save(data).$promise.then(function () {
              $route.reload();
            });
          });
      };

      $scope.deleteOpalCredential = function (opalCredential) {
        OpalCredentialResource.delete({id: opalCredential.opalUrl}).$promise.then(function () {
          $route.reload();
        });
      };

      $scope.editOpalCredentialCertificate = function(opalCredential) {
        $modal.open({
          templateUrl: 'app/config/views/config-modal-username-credential.html',
          controller: 'UsernamePasswordModalController',
          resolve: {
            opalCredential: function () {
              return opalCredential;
            }
          }
        }).result.then(function (data) {
            OpalCredentialsResource.save(data).$promise.then(function () {
              $route.reload();
            });
          });
      };

      $scope.downloadOpalCredentialCertificate = function (opalCredential) {
        $window.open('ws/config/opal-credential/' + encodeURIComponent(opalCredential.opalUrl) + '/certificate', '_blank', '');
      };
    }])

  .controller('ImportKeyPairModalController', ['$scope', '$location', '$modalInstance', 'isOpalCredential',
    function($scope, $location, $modalInstance, isOpalCredential) {

      $scope.isOpalCredential = isOpalCredential;

      $scope.credential = { opalUrl: '' };

      $scope.keyForm = {
        privateImport: '',
        publicImport: '',
        keyType: 0
      };

      $scope.save = function () {
        var data = isOpalCredential ? {type: 1, opalUrl: $scope.credential.opalUrl, keyForm: $scope.keyForm} : $scope.keyForm;
        $modalInstance.close(data);
      };

      $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
      };
    }])

  .controller('CreateKeyPairModalController', ['$scope', '$location', '$modalInstance', 'isOpalCredential',
    function($scope, $location, $modalInstance, isOpalCredential) {

      $scope.isOpalCredential = isOpalCredential;

      $scope.showAdvanced = false;

      $scope.credential = {opalUrl: ''};

      $scope.keyForm = {
        privateForm: {
          algo: 'RSA',
          size: 2048
        },
        publicForm: {},
        keyType: 0
      };

      $scope.save = function () {
        var data = isOpalCredential ? {type: 1, opalUrl: $scope.credential.opalUrl, keyForm: $scope.keyForm} : $scope.keyForm;

        $modalInstance.close(data);
      };

      $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
      };
    }])

  .controller('UsernamePasswordModalController', ['$scope', '$location',
    '$modalInstance', 'opalCredential',

    function($scope, $location, $modalInstance, opalCredential) {
      $scope.credential = opalCredential || {opalUrl: '', username: '', password: '', type: 0};

      $scope.save = function (form) {
        if ($scope.credential.confirm !== $scope.credential.password) {
          form.confirm.$invalid = true;
          form.$invalid = true;
          form.saveAttempted = true;
          return;
        }

        $modalInstance.close($scope.credential);
      };

      $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
      };
    }])

  .controller('MicaConfigEditController', ['$scope', '$resource', '$location', '$log',
    'MicaConfigResource', 'FormServerValidation',

    function ($scope, $resource, $location, $log, MicaConfigResource, FormServerValidation) {

      $scope.micaConfig = MicaConfigResource.get();
      $scope.availableLanguages = $resource('ws/config/languages').get();

      $scope.save = function () {

        if (!$scope.form.$valid) {
          $scope.form.saveAttempted = true;
          return;
        }

        $scope.micaConfig.$save(
          function () {
            $location.path('/config').replace();
          },
          function (response) {
            FormServerValidation.error(response, $scope.form);
          });
      };

    }]);
