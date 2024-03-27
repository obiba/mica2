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

/* global CONTACT_SCHEMA */
/* global CONTACT_DEFINITION */

mica.contact

  .constant('CONTACT_EVENTS', {
    contactUpdated: 'event:contact-updated',
    contactEditionCanceled: 'event:contact-edition-canceled',
    addInvestigator: 'event:add-investigator',
    addContact: 'event:add-contact',
    contactDeleted: 'event:contact-deleted'
  })

  .controller('ContactViewModalController', ['$scope', '$uibModalInstance', '$filter', 'ContactSerializationService', 'LocalizedSchemaFormService', 'micaConfig', 'contact',
    function ($scope, $uibModalInstance, $filter, ContactSerializationService, LocalizedSchemaFormService, micaConfig, contact) {
      var formLanguages = {};
      micaConfig.languages.forEach(function (loc) {
        formLanguages[loc] = $filter('translate')('language.' + loc);
      });

      $scope.sfOptions = {formDefaults: {languages: formLanguages, readonly: true}};
      $scope.sfForm = {schema: LocalizedSchemaFormService.translate(angular.copy(CONTACT_SCHEMA)),
        definition: LocalizedSchemaFormService.translate(angular.copy(CONTACT_DEFINITION()))};
      $scope.contact = contact;
      $scope.close = function () {
        $uibModalInstance.dismiss('close');
      };
      $scope.PersonTypeToResourceMap = mica.commons.consts.PersonTypeToResourceMap;
    }])

  .controller('ContactEditModalController', ['$scope',
    '$uibModalInstance',
    '$translate',
    '$filter',
    '$q',
    'ContactSerializationService',
    'LocalizedSchemaFormService',
    'ContactsSearchResource',
    'AlertService',
    'micaConfig',
    'contact',
    'excludes',
    'type',
    function ($scope,
              $uibModalInstance,
              $translate,
              $filter,
              $q,
              ContactSerializationService,
              LocalizedSchemaFormService,
              ContactsSearchResource,
              AlertService,
              micaConfig,
              contact,
              excludes,
              type) {
      $translate('contact.label.' + type).then(function(translation) {
        $scope.type = translation;
      });

      var validate = function() {
        console.log('Validate');
        new mica.commons.PersonsDuplicateFinder($q, ContactsSearchResource)
          .searchPerson($scope.selected.contact || {})
          .then(result => {
            if (result.status !== mica.commons.PERSON_DUPLICATE_STATUS.OK) {
              AlertService.alert({
                id: 'ContactModalController',
                type: result.status === mica.commons.PERSON_DUPLICATE_STATUS.WARNING ?  'warning' : 'danger',
                msgKey: result.messageKey,
                msgArgs: result.messageArgs
              });

            }

            $scope.validated = result.status !== mica.commons.PERSON_DUPLICATE_STATUS.ERROR;
          })
          .catch(error => console.error('Error', error));
      };

      var save = function (form) {
        $scope.$broadcast('schemaFormValidate');
        if (form.$valid) {
          $uibModalInstance.close(ContactSerializationService.serialize($scope.selected.contact));
        } else {
          $scope.form = form;
          $scope.form.saveAttempted = true;
        }
      };

      var cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };


      function onPersonSelected(value) {
        console.debug('Got person', value);
        $scope.selected.contact = value;
      }

      function onInstitutionSelected(value) {
        console.debug('Got institution', value);
        $scope.selected.contact.institution = value;
      }

      function onFormChanged() {
        $scope.validated = false;
      }

      var formLanguages = {};
      micaConfig.languages.forEach(function (loc) {
        formLanguages[loc] = $filter('translate')('language.' + loc);
      });

      $scope.validated = true;
      $scope.sfOptions = {formDefaults: {languages: formLanguages}};
      $scope.sfForm = {schema: LocalizedSchemaFormService.translate(angular.copy(CONTACT_SCHEMA)),
      definition: LocalizedSchemaFormService.translate(angular.copy(CONTACT_DEFINITION(onFormChanged.bind($scope))))};
      $scope.isNew = Object.getOwnPropertyNames(contact).length === 0;
      $scope.selected = {contact: contact};
      $scope.excludes = excludes;
      $scope.autoRefreshed = false;
      $scope.lang = $translate.use();
      $scope.validate = validate;
      $scope.save = save;
      $scope.cancel = cancel;
      $scope.onPersonSelected = onPersonSelected;
      $scope.onInstitutionSelected = onInstitutionSelected;
      $scope.onFormChanged = onFormChanged;
    }]);

