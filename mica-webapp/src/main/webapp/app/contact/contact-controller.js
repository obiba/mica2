/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

mica.contact

  .constant('CONTACT_EVENTS', {
    contactUpdated: 'event:contact-updated',
    contactEditionCanceled: 'event:contact-edition-canceled',
    addInvestigator: 'event:add-investigator',
    addContact: 'event:add-contact',
    contactDeleted: 'event:contact-deleted'
  })

  .controller('ContactController', ['$rootScope', '$scope', '$uibModal', '$translate', 'MicaConfigResource', 'CONTACT_EVENTS', 'NOTIFICATION_EVENTS',
    function ($rootScope, $scope, $uibModal, $translate, MicaConfigResource, CONTACT_EVENTS, NOTIFICATION_EVENTS) {
      $scope.micaConfig = MicaConfigResource.get();
      $scope.viewContact = function (contact) {
        if(!$scope.isOrderingContacts) {
          $uibModal.open({
            templateUrl: 'app/contact/contact-modal-view.html',
            controller: 'ContactViewModalController',
            resolve: {
              micaConfig: function() {
                return $scope.micaConfig;
              },
              contact: function () {
                return contact;
              }
            }
          });
        }
      };

      $scope.editMember = function (contactable, contact, type) {
        $scope.editMemberModal(contactable, contact, type);
      };

      var findContacts = function (contactable, type) {
        if (contactable.memberships) {
          return (contactable.memberships.filter(function (m) {
            return m.role === type;
          })[0] || {members: []}).members.map(function (p) {
              return p.id;
            });
        }

        return null;
      };

      $scope.editMemberModal = function (contactable, contact, type) {
        $uibModal
          .open({
            templateUrl: 'app/contact/contact-modal-form.html',
            controller: 'ContactEditModalController',
            resolve: {
              contact: function () {
                return contact;
              },
              excludes: function() {
                return [];
              },
              micaConfig: function() {
                return $scope.micaConfig;
              },
              type: function() {
                return type;
              }
            }
          })
          .result.then(function (contact) {
            $scope.$emit(CONTACT_EVENTS.contactUpdated, contactable, contact, type);
          }, function () {
            $scope.$emit(CONTACT_EVENTS.contactEditionCanceled, contactable);
          });
      };

      $scope.addMember = function (contactable, type) {
        $scope.addMemberModal(contactable, findContacts(contactable, type), type);
      };

      $scope.addMemberModal = function (contactable, excludes, type) {
        $uibModal
          .open({
            templateUrl: 'app/contact/contact-modal-form.html',
            controller: 'ContactEditModalController',
            resolve: {
              contact: function () {
                return {};
              },
              excludes: function () {
                return excludes;
              },
              micaConfig: function() {
                return $scope.micaConfig;
              },
              type: function() {
                return type;
              }
            }
          })
          .result.then(function (contact) {
            $scope.$emit(CONTACT_EVENTS.addContact, contactable, contact, type);
          }, function () {
            $scope.$emit(CONTACT_EVENTS.contactEditionCanceled, contactable);
          });
      };

      $scope.deleteMember = function (contactable, contact, type) {
        $scope.deleteInvestigatorOrContact(contactable, contact, type);
      };

      $scope.deleteInvestigatorOrContact = function (contactable, contact, type) {
        var titleKey = 'contact.delete.member.title';
        var messageKey = 'contact.delete.member.confirm';

        $translate([titleKey, messageKey], {
          name: [contact.title, contact.firstName, contact.lastName].filter(function (i) { return i; }).join(' '),
          type: type
        })
          .then(function (translation) {
            $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
              {title: translation[titleKey], message: translation[messageKey]},
              contact);
          });

        $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, function (event, contactConfirmed) {
          if (contactConfirmed === contact) {
            $scope.$emit(CONTACT_EVENTS.contactDeleted, contactable, contact, type);
          }
        });
      };
    }])

  .controller('ContactViewModalController', ['$scope', '$uibModalInstance', 'micaConfig', 'contact',
    function ($scope, $uibModalInstance, micaConfig, contact) {
      $scope.contact = contact;
      $scope.tabs = (micaConfig.languages || []).map(function (lang) {
        return {lang: lang, labelKey: 'language.' + lang};
      });

      $scope.close = function () {
        $uibModalInstance.dismiss('close');
      };
    }])

  .controller('ContactEditModalController', ['$scope',
    '$uibModalInstance',
    '$translate',
    'ContactsSearchResource',
    'micaConfig',
    'contact',
    'excludes',
    'type',
    function ($scope, $uibModalInstance, $translate, ContactsSearchResource, micaConfig, contact, excludes, type) {
      $translate(type).then(function(tranlation) {
        $scope.type = tranlation;
      });

      var newResult = function() {
        return {persons: [], total: 0, current: 0};
      };

      var save = function (form) {
        if (form.$valid) {
          $uibModalInstance.close($scope.selected.contact);
        } else {
          $scope.form = form;
          $scope.form.saveAttempted = true;
        }
      };

      var cancel = function () {
        $uibModalInstance.dismiss('cancel');
      };

      var findContacts = function(search) {
        if (search) {
          if (!$scope.autoRefreshed) {
            $scope.result = newResult();
          }

          ContactsSearchResource.search(
            {
              query: search + '*',
              exclude: $scope.excludes,
              from: $scope.result.persons.length
            },
            function onSuccess(result) {
              if (result.persons) {
                $scope.result.persons = $scope.result.persons.concat(result.persons);
                $scope.result.total = result.total;
                $scope.result.current = $scope.result.persons.length;
                $scope.autoRefreshed = false;
              }
            });
        }
        else {
          $scope.result = newResult();
        }
      };

      var onHighlighted = function(index, isLast, search) {
        if (isLast && !$scope.autoRefreshed) {
          $scope.autoRefreshed = true;
          $scope.findContacts(search);
        }
      };

      $scope.tabs = (micaConfig.languages || []).map(function (lang) {
        return {lang: lang, labelKey: 'language.' + lang};
      });
      $scope.isNew = Object.getOwnPropertyNames(contact).length === 0;
      $scope.selected = {contact: contact};
      $scope.excludes = excludes;
      $scope.result = newResult();
      $scope.autoRefreshed = false;
      $scope.save = save;
      $scope.cancel = cancel;
      $scope.findContacts = findContacts;
      $scope.onHighlighted = onHighlighted;
    }]);

