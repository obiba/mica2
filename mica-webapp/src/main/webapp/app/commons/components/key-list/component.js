/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

(function () {

  function ModalController ($scope, $uibModalInstance, keys, key) {
    function hasSpecialCharacters(str) {
      if (str && typeof str === 'string') {
        var result = str.match (/[A-Z\s~!\.]/g);
        return result;
      }

      return false;
    }

    function save(form) {
      $scope.data.key = $scope.data.key.toLowerCase();
      form.key.$setValidity ('text', !keys || keys.indexOf ($scope.data.key) < 0);
      form.key.$setValidity ('character', !hasSpecialCharacters ($scope.data.key));

      if (form.$valid) {
        $uibModalInstance.close ($scope.data.key);
      } else {
        $scope.form = form;
        $scope.form.saveAttempted = true;
      }
    }

    function cancel() {
      $uibModalInstance.dismiss ('cancel');
    }

    $scope.cancel = cancel;
    $scope.save = save;
    $scope.title = key === null ? 'key-list.add-key' : 'key-list.edit-key';
    $scope.data = {key: key ? key : null};
  }

  function Controller ($rootScope, $uibModal, $translate, NOTIFICATION_EVENTS) {
    var ctrl = this;

    function addOrEdit(index) {
      var hasIndex = angular.isDefined(index);
      $uibModal
        .open ({
          templateUrl: 'app/commons/components/key-list/modal.html',
          controller: ['$scope', '$uibModalInstance', 'keys', 'key', ModalController],
          resolve: {
            keys: function () {
              return ctrl.keys;
            },
            key: function() {
              return hasIndex ? ctrl.keys[index] : null;
            }
          }
        })
        .result.then (function (key) {
          ctrl.keys = ctrl.keys || [];

          if (hasIndex) {
            ctrl.keys[index] = key;
          } else {
            ctrl.keys.push (key);
          }

          ctrl.onUpdateKeys ({keys: ctrl.keys});
        });
    }

    function onConfirmDelete(event, index) {
      if (ctrl.keyToDelete === ctrl.keys[index]) {
        ctrl.keys.splice(index, 1);
        ctrl.onUpdateKeys ({keys: ctrl.keys});
        delete ctrl.keyToDelete;
      }

      ctrl.unbindOnConfirmDelete();
      ctrl.unbindOnConfirmDelete = null;
    }

    function remove(index) {
      ctrl.keyToDelete = ctrl.keys[index];
      var titleKey = 'key-list.delete-dialog.title';
      var messageKey = 'key-list.delete-dialog.message';
      var emitter = $rootScope.$new();
      ctrl.$on = angular.bind(emitter, emitter.$on);
      ctrl.unbindOnConfirmDelete = ctrl.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, onConfirmDelete.bind(this));

      $translate([titleKey, messageKey], {
        key: ctrl.keyToDelete
      }).then(function (translation) {
        $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
          {title: translation[titleKey], message: translation[messageKey]},
          index);
      });
    }

    ctrl.addOrEdit = addOrEdit;
    ctrl.remove = remove;
  }

  mica.commons.component ('keyList', {
    bindings: {
      titleKey: '@',
      helpKey: '@',
      keys: '<',
      onUpdateKeys: '&'
    },
    templateUrl: 'app/commons/components/key-list/component.html',
    controller: ['$rootScope', '$uibModal', '$translate', 'NOTIFICATION_EVENTS', Controller]
  });
}) ();

