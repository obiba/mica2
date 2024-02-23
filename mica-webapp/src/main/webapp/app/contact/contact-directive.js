'use strict';

angular.module('mica.contact')
  .component('membershipManagement', {
    bindings: {
      doctype: '@',
      docid: '<',
      order: '<',
      permissions: '<',
      onUpdate: '&'
    },
    templateUrl: 'app/contact/views/contact-view.html',
    controller: ['NOTIFICATION_EVENTS', 'MicaConfigResource', 'PersonResource', 'ContactSerializationService', '$rootScope', '$scope', '$timeout', '$translate', '$uibModal',
      function MemberShipManagementController(NOTIFICATION_EVENTS, MicaConfigResource, PersonResource, ContactSerializationService, $rootScope, $scope, $timeout, $translate, $uibModal) {
        var ctrl = this;
        var listenerRegistry = new obiba.utils.EventListenerRegistry();

        ctrl.data = [];
        ctrl.memberships = {};
        ctrl.isOrdering = false;

        function initMembership(data, personMembershipsFieldName, currentDocid) {
          data.forEach(function (person) {
            (person[personMembershipsFieldName] || [])
            .filter(function (sMembership) {return sMembership.parentId === currentDocid;})
            .forEach(function (sMembership) {
              if (!ctrl.memberships[sMembership.role]) {
                ctrl.memberships[sMembership.role] = [person];
              } else {
                ctrl.memberships[sMembership.role].push(person);
              }
            });
          });

          if (ctrl.order) {
            ctrl.order.forEach(function (order) {
              if (order.role in ctrl.memberships) {
                ctrl.memberships[order.role].sort(function (a, b) {
                  return order.personIds.indexOf(a.id) - order.personIds.indexOf(b.id);
                });
              }
            });
          }
        }

        function refresh(currentDocid) {
          ctrl.data = [];
          ctrl.memberships = {};
          if (ctrl.doctype === 'STUDY') {
            return PersonResource.getStudyMemberships({studyId: currentDocid}).$promise.then(function (data) {
              ctrl.data = data;
              initMembership(data, 'studyMemberships', currentDocid);
              return data;
            });
          } else {
            return PersonResource.getNetworkMemberships({networkId: currentDocid}).$promise.then(function (data) {
              ctrl.data = data;
              initMembership(data, 'networkMemberships', currentDocid);
              return data;
            });
          }
        }

        function viewPerson(person) {
          if (!ctrl.isOrdering) {
            $uibModal.open({
              templateUrl: 'app/contact/contact-modal-view.html',
              controller: 'ContactViewModalController',
              resolve: {
                micaConfig: function() {
                  return ctrl.micaConfig;
                },
                contact: function () {
                  return ContactSerializationService.deserialize(person);
                }
              }
            });
          }
        }

        function editPerson(person, role) {
          $uibModal
          .open({
            templateUrl: 'app/contact/contact-modal-form.html',
            controller: 'ContactEditModalController',
            resolve: {
              contact: function () {
                return ContactSerializationService.deserialize(person);
              },
              excludes: function() {
                return [];
              },
              micaConfig: function() {
                return ctrl.micaConfig;
              },
              type: function() {
                return role;
              }
            }
          }).result.then(function (data) {
            data.$update().then(function () {
              refresh(ctrl.docid).then(function () {
                orderUpdated();
              });
            });
          });
        }

        function removeRole(person, role) {
          var titleKey = 'contact.delete.member.title';
          var messageKey = 'contact.delete.member.confirm';

          $translate([titleKey, messageKey], {
            name: [person.title, person.firstName, person.lastName].filter(function (i) { return i; }).join(' '),
            type: role
          }).then(function (translation) {
            $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
              {title: translation[titleKey], message: translation[messageKey]},
              person);
          });

          listenerRegistry.register($scope.$on(NOTIFICATION_EVENTS.confirmDialogRejected, function () {
            listenerRegistry.unregisterAll();
          }));

          listenerRegistry.register($scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, function (event, contactConfirmed) {
            listenerRegistry.unregisterAll();


            if (contactConfirmed === person) {
              if (ctrl.doctype === 'STUDY') {
                person.studyMemberships = person.studyMemberships.filter(function (membership) {
                  return membership.parentId === ctrl.docid && membership.role !== role;
                });
              } else {
                person.networkMemberships = person.networkMemberships.filter(function (membership) {
                  return membership.parentId === ctrl.docid && membership.role !== role;
                });
              }

              person.$update().then(function () {
                refresh(ctrl.docid).then(function () {
                  orderUpdated();
                });
              });
            }
          }));
        }

        function addPerson(role) {
          var excludes = (ctrl.memberships[role] || []).map(function (member) {
            return member.id;
          });

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
                return ctrl.micaConfig;
              },
              type: function() {
                return role;
              }
            }
          }).result.then(function (person) {
            var newMembership = {
              role: role,
              parentId: ctrl.docid
            };

            if (ctrl.doctype === 'STUDY') {
              if (!person.studyMemberships) {
                person.studyMemberships = [newMembership];
              } else {
                person.studyMemberships.push(newMembership);
              }
            } else {
              if (!person.networkMemberships) {
                person.networkMemberships = [newMembership];
              } else {
                person.networkMemberships.push(newMembership);
              }
            }

            if (person.id) {
              PersonResource.update(person).$promise.then(function (data) {
                if (data) {
                  refresh(ctrl.docid).then(function () {
                    orderUpdated();
                  });
                }
              });
            } else {
              PersonResource.create(person).$promise.then(function (data) {
                if (data && data.id) {
                  refresh(ctrl.docid).then(function () {
                    orderUpdated();
                  });
                }
              });
            }
          });
        }

        function orderUpdated() {
          var order = [];
          for (var role in ctrl.memberships) {
            var people = {
              role: role,
              personIds: (ctrl.memberships[role] || []).map(function (item) {
                return item.id;
              })
            };
            order.push(people);
          }

          ctrl.onUpdate({newOrder: order});
        }

        ctrl.sortableOptions = {
          start: function() {
            ctrl.isOrdering = true;
          },
          stop: function (event, ui) {
            if ('dropindex' in ui.item.sortable  && ui.item.sortable.index !== ui.item.sortable.dropindex) {
              orderUpdated();
              $timeout(function () {
                ctrl.isOrdering = false;
              });
            }
          }
        };

        ctrl.$onInit = function() {
          ctrl.micaConfig = MicaConfigResource.get();
        };

        ctrl.$onChanges = function (changesObj) {
          if (changesObj.docid) {
            var currentDocid = changesObj.docid.currentValue;
            if (currentDocid) {
              refresh(currentDocid);
            }
          }
        };

        ctrl.view = viewPerson;
        ctrl.edit = editPerson;
        ctrl.remove = removeRole;
        ctrl.add = addPerson;
    }]
  });
