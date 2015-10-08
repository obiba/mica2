'use strict';

mica.contact

  .constant('CONTACT_EVENTS', {
    contactUpdated: 'event:contact-updated',
    contactEditionCanceled: 'event:contact-edition-canceled',
    addInvestigator: 'event:add-investigator',
    addContact: 'event:add-contact',
    contactDeleted: 'event:contact-deleted'
  })

  .controller('ContactController', ['$rootScope', '$scope', '$modal', '$translate', 'CONTACT_EVENTS', 'NOTIFICATION_EVENTS',
    function ($rootScope, $scope, $modal, $translate, CONTACT_EVENTS, NOTIFICATION_EVENTS) {

      $scope.viewContact = function (contact) {
        $modal.open({
          templateUrl: 'app/contact/contact-modal-view.html',
          controller: 'ContactViewModalController',
          resolve: {
            contact: function () {
              return contact;
            }
          }
        });
      };

      $scope.editInvestigator = function (contactable, contact) {
        $scope.editInvestigatorOrContact(contactable, contact, true);
      };

      $scope.editContact = function (contactable, contact) {
        $scope.editInvestigatorOrContact(contactable, contact, false);
      };

      var findContacts = function(contactable, type) {
        if (contactable[type]) {
          return contactable[type].map(function(contact) {
            return contact.id;
          });
        }

        return null;
      };

      $scope.editInvestigatorOrContact = function (contactable, contact, isInvestigator) {
        $modal
          .open({
            templateUrl: 'app/contact/contact-modal-form.html',
            controller: 'ContactEditModalController',
            resolve: {
              contact: function () {
                return contact;
              },
              isInvestigator: function () {
                return isInvestigator;
              },
              excludes: function() {
                return [];
              }
            }
          })
          .result.then(function (contact) {
            $scope.$emit(CONTACT_EVENTS.contactUpdated, contactable, contact);
          }, function () {
            $scope.$emit(CONTACT_EVENTS.contactEditionCanceled, contactable);
          });
      };

      $scope.addInvestigator = function (contactable) {
        $scope.addInvestigatorOrContact(contactable, true, findContacts(contactable, 'investigators'));
      };

      $scope.addContact = function (contactable) {
        $scope.addInvestigatorOrContact(contactable, false, findContacts(contactable, 'contacts'));
      };

      $scope.addInvestigatorOrContact = function (contactable, isInvestigator, excludes) {
        $modal
          .open({
            templateUrl: 'app/contact/contact-modal-form.html',
            controller: 'ContactEditModalController',
            resolve: {
              contact: function () {
                return {};
              },
              isInvestigator: function () {
                return isInvestigator;
              },
              excludes: function () {
                return excludes;
              }
            }
          })
          .result.then(function (contact) {
            if (isInvestigator) {
              $scope.$emit(CONTACT_EVENTS.addInvestigator, contactable, contact);
            } else {
              $scope.$emit(CONTACT_EVENTS.addContact, contactable, contact);
            }
          }, function () {
            $scope.$emit(CONTACT_EVENTS.contactEditionCanceled, contactable);
          });
      };

      $scope.deleteInvestigator = function (contactable, contact) {
        $scope.deleteInvestigatorOrContact(contactable, contact, true);
      };

      $scope.deleteContact = function (contactable, contact) {
        $scope.deleteInvestigatorOrContact(contactable, contact, false);
      };

      $scope.deleteInvestigatorOrContact = function (contactable, contact, isInvestigator) {

        var titleKey = 'contact.delete.' + (isInvestigator ? 'investigator' : 'contact') + '.title';
        var messageKey = 'contact.delete.' + (isInvestigator ? 'investigator' : 'contact') + '.confirm';
        $translate([titleKey, messageKey], { name: contact.title + ' ' + contact.firstName + ' ' + contact.lastName })
          .then(function (translation) {
            $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
              {title: translation[titleKey], message: translation[messageKey]},
              contact);
          });

        $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, function (event, contactConfirmed) {
          if (contactConfirmed === contact) {
            $scope.$emit(CONTACT_EVENTS.contactDeleted, contactable, contact, isInvestigator);
          }
        });
      };

    }])

  .controller('ContactViewModalController', ['$scope', '$modalInstance', 'MicaConfigResource', 'contact',
    function ($scope, $modalInstance, MicaConfigResource, contact) {

      $scope.contact = contact;

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({ lang: lang, labelKey: 'language.' + lang });
        });
      });

      $scope.close = function () {
        $modalInstance.dismiss('close');
      };

    }])

  .controller('ContactEditModalController', ['$scope', '$modalInstance', 'ContactsSearchResource','MicaConfigResource', 'contact', 'isInvestigator', 'excludes',
    function ($scope, $modalInstance, ContactsSearchResource, MicaConfigResource, contact, isInvestigator, excludes) {

      $scope.isNew = Object.getOwnPropertyNames(contact).length === 0;
      $scope.selected = { contact: contact };
      $scope.isInvestigator = isInvestigator;
      $scope.excludes = excludes;
      $scope.contacts = [];

      MicaConfigResource.get(function (micaConfig) {
        $scope.tabs = [];
        micaConfig.languages.forEach(function (lang) {
          $scope.tabs.push({ lang: lang, labelKey: 'language.' + lang });
        });
      });

      $scope.save = function (form) {
        if (form.$valid) {
          $modalInstance.close($scope.selected.contact);
        } else {
          $scope.form = form;
          $scope.form.saveAttempted = true;
        }
      };

      $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
      };

      $scope.findContacts = function(search) {
        if (search) {
          ContactsSearchResource.search({query: search + '*', 'exclude': $scope.excludes},
            function onSuccess(result) {
              $scope.contacts = result.contacts;
            });
        }
      };

    }]);

