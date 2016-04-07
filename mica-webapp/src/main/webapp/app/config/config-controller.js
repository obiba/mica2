'use strict';

mica.config
  .controller('MicaConfigController', ['$rootScope', '$scope', '$resource', '$route', '$window', '$log', 'MicaConfigResource', '$uibModal', '$translate',
    'OpalCredentialsResource', 'OpalCredentialResource', 'KeyStoreResource', 'FormServerValidation', 'NOTIFICATION_EVENTS',

    function ($rootScope, $scope, $resource, $route, $window, $log, MicaConfigResource, $uibModal, $translate,
              OpalCredentialsResource, OpalCredentialResource, KeyStoreResource, FormServerValidation,
              NOTIFICATION_EVENTS) {
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
        $uibModal.open({
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
        $uibModal.open({
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
        $uibModal.open({
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
        $uibModal.open({
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
        $uibModal.open({
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
        $uibModal.open({
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

      var roleToDelete;

      $scope.deleteRole = function (index) {
        var titleKey = 'config.delete-role-title';
        var messageKey = 'config.delete-role-confirm';

        roleToDelete = $scope.micaConfig.roles[index];
        $translate([titleKey, messageKey], {
          role: roleToDelete
        })
          .then(function (translation) {
            $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
              {title: translation[titleKey], message: translation[messageKey]},
              index);
          });
      };

      $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, function (event, index) {
        if (roleToDelete === $scope.micaConfig.roles[index]) {
          $scope.micaConfig.roles.splice(index, 1);

          $scope.micaConfig.$save(
            function () {
              $route.reload();
            },
            function (response) {
              FormServerValidation.error(response, $scope.form);
            });
        }
      });

      $scope.addRole = function() {
        $uibModal
          .open({
            templateUrl: 'app/config/views/config-roles-modal-form.html',
            controller: 'RoleModalController',
            resolve: {
              micaConfig: function() {
                return $scope.micaConfig;
              },
              role: function () {
                return null;
              }
            }
          })
          .result.then(function (role) {
            $scope.micaConfig.roles = $scope.micaConfig.roles || [];
            $scope.micaConfig.roles.push(role);

            $scope.micaConfig.$save(
              function () {
                $route.reload();
              },
              function (response) {
                FormServerValidation.error(response, $scope.form);
              });
          }, function () {
          });
      };
    }])

  .controller('ImportKeyPairModalController', ['$scope', '$location', '$uibModalInstance', 'isOpalCredential',
    function($scope, $location, $uibModalInstance, isOpalCredential) {
      $scope.isOpalCredential = isOpalCredential;
      $scope.credential = {opalUrl: ''};
      $scope.keyForm = {
        privateImport: '',
        publicImport: '',
        keyType: 0
      };

      $scope.save = function (form) {
        if (form.$valid) {
          var data = isOpalCredential ? {
            type: 1,
            opalUrl: $scope.credential.opalUrl,
            keyForm: $scope.keyForm
          } : $scope.keyForm;
          $uibModalInstance.close(data);
        }

        form.saveAttempted = true;
      };

      $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };
    }])

  .controller('CreateKeyPairModalController', ['$scope', '$location', '$uibModalInstance', 'isOpalCredential',
    function($scope, $location, $uibModalInstance, isOpalCredential) {

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

      $scope.save = function (form) {
        if(form.$valid) {
          var data = isOpalCredential ? {
            type: 1,
            opalUrl: $scope.credential.opalUrl,
            keyForm: $scope.keyForm
          } : $scope.keyForm;

          $uibModalInstance.close(data);
        }

        form.saveAttempted = true;
      };

      $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };
    }])

  .controller('RoleModalController', ['$scope', '$uibModalInstance', '$log', 'micaConfig', 'role',
    function ($scope, $uibModalInstance, $log, micaConfig, role) {
      var oldRole = role;
      $scope.role = {id: role};

      $scope.save = function (form) {
        form.id.$setValidity('text', !micaConfig.roles || micaConfig.roles.indexOf($scope.role.id) < 0 || $scope.role.id === oldRole);

        if (form.$valid) {
          $uibModalInstance.close($scope.role.id);
        } else {
          $scope.form = form;
          $scope.form.saveAttempted = true;
        }
      };

      $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };
    }])

  .controller('UsernamePasswordModalController', ['$scope', '$location',
    '$uibModalInstance', 'opalCredential',

    function($scope, $location, $uibModalInstance, opalCredential) {
      $scope.credential = opalCredential || {opalUrl: '', username: '', password: '', type: 0};

      $scope.save = function (form) {
        if(form.$valid) {
          if ($scope.credential.confirm !== $scope.credential.password) {
            form.confirm.$invalid = true;
            form.$invalid = true;
            form.saveAttempted = true;
            return;
          }

          $uibModalInstance.close($scope.credential);
        }

        form.saveAttempted = true;
      };

      $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };
    }])

  .controller('MicaConfigEditController', ['$scope', '$resource', '$window', '$location', '$log',
    'MicaConfigResource', 'FormServerValidation',

    function ($scope, $resource, $window, $location, $log, MicaConfigResource, FormServerValidation) {
      var reload = false;
      $scope.micaConfig = MicaConfigResource.get();
      $scope.availableLanguages = $resource('ws/config/languages').get();

      $scope.micaConfig.$promise.then(function() {
        $scope.$watchGroup(['name', 'isNetworkEnabled', 'isSingleNetworkEnabled',
          'isSingleStudyEnabled', 'isStudyDatasetEnabled', 'isHarmonizationDatasetEnabled'].map(function(p) { return 'micaConfig.' + p; }), function(value, oldValue) {
          if(!angular.equals(value,oldValue)) {
            reload = true;
          }
        });
      });

      $scope.save = function () {

        if (!$scope.form.$valid) {
          $scope.form.saveAttempted = true;
          return;
        }

        $scope.micaConfig.$save(
          function () {
            $location.path('/admin/general');
            if(reload) {
              $window.location.reload();
            }
          },
          function (response) {
            FormServerValidation.error(response, $scope.form);
          });
      };

    }])

  .controller('MicaConfigStyleEditController', ['$scope', '$resource', '$window', '$location', '$log',
    'MicaConfigResource', 'FormServerValidation', 'StyleEditorService',

    function ($scope, $resource, $window, $location, $log, MicaConfigResource, FormServerValidation, StyleEditorService) {
      var reload = false;
      $scope.micaConfig = MicaConfigResource.get();

      $scope.micaConfig.$promise.then(function() {
        $scope.$watch('micaConfig.style', function(value, oldValue) {
          if(!angular.equals(value,oldValue)) {
            reload = true;
          }
        });
      });

      StyleEditorService.configureAcePaths();
      $scope.ace = StyleEditorService.getEditorOptions();

      $scope.save = function () {

        $scope.micaConfig.$save(
          function () {
            $location.path('/admin');
            if(reload) {
              $window.location.reload();
            }
          },
          function (response) {
            FormServerValidation.error(response, $scope.form);
          });
      };

    }]);
