/*
 * Copyright (c) 2020 OBiBa. All rights reserved.
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

(function () {
  const MODE = {
    VIEW: 'view',
    EDIT: 'edit',
    NEW: 'new',
    REVISIONS: 'revisions'
  };

  class PersonViewController {
    constructor($rootScope,
                $scope,
                $location,
                $routeParams,
                $filter,
                $uibModal,
                $q,
                screenSize,
                EntityMembershipService,
                LocalizedSchemaFormService,
                MicaConfigResource,
                PersonResource,
                ContactsSearchResource,
                ContactSerializationService,
                NOTIFICATION_EVENTS,
                FormDirtyStateObserver,
                EntityTitleService,
                AlertService) {
      this.$rootScope = $rootScope;
      this.$scope = $scope;
      this.$location = $location;
      this.$routeParams = $routeParams;
      this.$filter = $filter;
      this.$uibModal = $uibModal;
      this.$q = $q;
      this.LocalizedSchemaFormService = LocalizedSchemaFormService;
      this.EntityMembershipService = EntityMembershipService;
      this.MicaConfigResource = MicaConfigResource;
      this.PersonResource = PersonResource;
      this.ContactsSearchResource = ContactsSearchResource;
      this.ContactSerializationService = ContactSerializationService;
      this.NOTIFICATION_EVENTS = NOTIFICATION_EVENTS;
      this.FormDirtyStateObserver = FormDirtyStateObserver;
      this.EntityTitleService = EntityTitleService;
      this.AlertService = AlertService;
      this.validated = true;
      this.screenSize = screenSize;
      this.screen = {size: null, device: null};
    }

    __getScreenSize() {
      var size = ['lg', 'md', 'sm', 'xs'].filter(function (size) {
        return this.screenSize.is(size);
      });

      this.screen.size = size ? size[0] : 'lg';
      this.screen.device = this.screenSize.is('md, lg') ? 'desktop' : 'mobile';
      this.screen.is = this.screenSize.is;
    }

    __getMode() {
      let mode = MODE.VIEW;
      const parts = this.$location.path().match(/\/(new|edit|revisions)$/);

      if (parts) {
        const modePart = parts[1];
        switch (modePart) {
          case MODE.NEW:
          case MODE.EDIT:
          case MODE.REVISIONS:
            mode = modePart;
            break;
        }
      }

      return mode;
    }

    __getFullname() {
      return `${this.person.firstName} ${this.person.lastName}`.trim();
    }

    __initMembershipsTableData() {
      this.memberships = {};
      if (this.person.networkMemberships) {
        this.memberships.networks = this.EntityMembershipService.groupRolesByEntity('networks', this.person.networkMemberships);
      }

      if (this.person.studyMemberships) {
        this.memberships.studies =
          this.EntityMembershipService.groupRolesByEntity(
            'studies',
            this.person.studyMemberships.filter(membership => membership['obiba.mica.PersonDto.StudyMembershipDto.meta'].type ==='individual-study'));

        this.memberships.initiatives = this.EntityMembershipService.groupRolesByEntity(
          'initiatives',
          this.person.studyMemberships.filter(membership => membership['obiba.mica.PersonDto.StudyMembershipDto.meta'].type !=='individual-study'));
      }
    }

    __initPerson(person) {
      this.person = this.ContactSerializationService.deserialize(person);
      this.__initMembershipsTableData();
    }

    __getPerson(id) {
      this.PersonResource.get({id}).$promise.then(person => {
        this.__initPerson(person);
      });
    }

    __observeFormDirtyState() {
      if (MODE.VIEW !== this.mode) {
        this.FormDirtyStateObserver.observe(this.$scope);
      }
    }

    __unobserveFormDirtyState() {
      if (MODE.VIEW !== this.mode) {
        this.FormDirtyStateObserver.unobserve();
      }
    }

    __deletePerson(id) {
      this.PersonResource.delete({id}).$promise
        .then(() => {
          this.listenerRegistry.unregisterAll();
          this.navigateOut('/persons', id);
        });
    }

    __updatePerson() {
      this.PersonResource.update(this.ContactSerializationService.serialize(this.person)).$promise
        .then((person) => {
          this.listenerRegistry.unregisterAll();
          this.__initPerson(person);
        });
    }

    __deleteEntity(entityType, entity) {
      let memberships = this.person[`${entityType}Memberships`];
      memberships = memberships.filter((item) => item.parentId !== entity.id);
      if (memberships.length === 0) {
        delete this.person[`${entityType}Memberships`];
      } else {
        this.person[`${entityType}Memberships`] = memberships;
      }

      this.__updatePerson();
    }

    __onDelete(titleKey, titleArgs, msgKey, msgArgs, callback) {
      this.listenerRegistry.register(
        this.$scope.$on(this.NOTIFICATION_EVENTS.confirmDialogRejected, () => this.listenerRegistry.unregisterAll())
      );

      this.listenerRegistry.register(
        this.$scope.$on(this.NOTIFICATION_EVENTS.confirmDialogAccepted, callback)
      );

      this.$rootScope.$broadcast(this.NOTIFICATION_EVENTS.showConfirmDialog,
        {
          titleKey: titleKey,
          titleArgs: titleArgs,
          messageKey: msgKey,
          messageArgs: msgArgs
        }, {}
      );
    }

    __addMemberships(roles, entities, entityType) {
      let entityMemberships = this.person[`${entityType}Memberships`] || [];
      roles.forEach(role => {
        entities.forEach(entity => {
          let membership = {
            role,
            parentId: entity.id,
            parentAcronym: entity.acronym,
            parentName: entity.name
          };

          if ('study' === entityType) {
            membership['obiba.mica.PersonDto.StudyMembershipDto.meta'] = {type: entity.studyResourcePath};
          }
          entityMemberships.push(membership);
        });
      });

      this.person[`${entityType}Memberships`] = entityMemberships;
      this.__updatePerson();
    }

    __updateEntityRoles(selections) {
      const membershipEntity = selections.membershipEntities[0];
      let memberships = this.person[`${selections.entityType}Memberships`];
      // remove to recreate from scratch
      this.person[`${selections.entityType}Memberships`] =
        memberships.filter(membership => membership.parentId !== membershipEntity.parentId);

      selections.roles.forEach((role) => {
        let item = Object.assign({}, membershipEntity);
        item.role = role;
        this.person[`${selections.entityType}Memberships`].push(item);
      });

      this.__updatePerson();
    }

    __openMembershipsModal(fullname, roles, memberships, entitySearchResource, entityType, query) {
      const entitiesTitle = this.EntityTitleService.translate(entityType, true);
      this.$uibModal.open({
        templateUrl: 'app/persons/views/entity-list-modal.html',
        controllerAs: '$ctrl',
        controller: ['$uibModalInstance',
          function($uibModalInstance) {
            this.selectedRoles = [];
            this.selectedEntities = [];
            this.roles = roles;
            this.memberships = memberships || [];
            this.entitySearchResource = entitySearchResource;
            this.entityType = entityType;
            this.entitiesTitle = entitiesTitle;
            this.addDisabled = true;
            this.fullname = fullname;
            this.query = query;

            const updateAddDisable =
              () => this.addDisabled = this.selectedRoles.length < 1 || this.selectedEntities.length < 1;

            this.onRolesSelected = (selectedRoles) => {
              this.selectedRoles = selectedRoles;
              updateAddDisable.call(this);
            };

            this.onEntitiesSelected = (selectedEntities) => {
              this.selectedEntities = selectedEntities;
              updateAddDisable.call(this);
            };

            this.onAdd = () => $uibModalInstance.close(
              {
                roles: this.selectedRoles,
                entities: this.selectedEntities
              }
            );

            this.onClose = () => $uibModalInstance.dismiss('close');
          }]
      }).result.then(selections => {
        this.__addMemberships(selections.roles, selections.entities, entityType === 'initiative' ? 'study' : entityType);
      });
    }

    __openEntityRolesModal(roles, membershipEntities, entityType, entity) {
      const currentRoles = membershipEntities.map((item) => item.role);
      const entityTitle = this.EntityTitleService.translate(entityType);
      this.$uibModal.open({
        templateUrl: 'app/persons/views/entity-roles-modal.html',
        controllerAs: '$ctrl',
        controller: ['$uibModalInstance',
          function($uibModalInstance) {
            this.roles = roles;
            this.entityType = entityType;
            this.entityTitle = entityTitle;
            this.selectedRoles = currentRoles;
            this.entity = entity;
            this.membershipEntities = membershipEntities;

            this.onRolesSelected = (selectedRoles) => {
              this.selectedRoles = selectedRoles;
            };

            this.onUpdate = () => $uibModalInstance.close({
              roles: this.selectedRoles,
              membershipEntities: this.membershipEntities,
              entityType: this.entityType
            });

            this.onClose = () => $uibModalInstance.dismiss('close');
          }]
      }).result.then(selections => {
        this.__updateEntityRoles(selections);
      });
    }

    __initializeForm() {
      this.loading = false;
      this.MicaConfigResource.get().$promise.then(config => {
        this.listenerRegistry = new obiba.utils.EventListenerRegistry();
        this.mode = this.__getMode();
        this.config = config;
        const languages = this.config.languages.reduce((map, locale) => {
          map[locale] = this.$filter('translate')('language.' + locale);
          return map;
        }, {});

        this.sfOptions = {
          formDefaults: {
            languages: languages,
            readonly: MODE.VIEW === this.mode
          }
        };

        this.sfForm = {
          schema: this.LocalizedSchemaFormService.translate(angular.copy(CONTACT_SCHEMA)),
          definition: this.LocalizedSchemaFormService.translate(angular.copy(CONTACT_DEFINITION(this.onFormChanged.bind(this))))
        };

        if (MODE.NEW !== this.mode) {
          this.__getPerson(this.$routeParams.id);
        } else {
          this.person = {};
        }

        this.__observeFormDirtyState();
        this.loading = false;
      });
    }

    onFormChanged() {
      this.validated = false;
    }

    onInstitutionSelected(value) {
      this.person.institution = value;
    }

    navigateOut(path, exclude) {
      this.__unobserveFormDirtyState();
      let search = this.pagination || {};
      if (exclude) {
        search.exclude = exclude;
      }
      this.$location.path(path).search(search).replace();
    }

    $onInit() {
      this.$translateChangeSuccessHandler = this.$rootScope.$on('$translateChangeSuccess', () => this.__initializeForm());
      this.__initializeForm();
      this.pagination = this.$location.search();
      this.$location.search({}).replace();
    }

    $onDestroy() {
      this.$translateChangeSuccessHandler();
    }

    onCancel() {
      switch (this.mode) {
        case MODE.NEW:
          this.navigateOut('/persons');
          break;
        case MODE.EDIT:
          this.navigateOut(`/person/${this.$routeParams.id}`);
          break;
      }
    }

    onValidate() {
      this.$scope.$broadcast('schemaFormValidate');
      if (this.$scope.form.$valid) {
        new mica.commons.PersonsDuplicateFinder(this.$q, this.ContactsSearchResource)
          .searchPerson(this.person)
          .then(result => {
            if (result.status !== mica.commons.PERSON_DUPLICATE_STATUS.OK) {
              this.AlertService.alert({
                id: 'PersonViewComponent',
                type: result.status === mica.commons.PERSON_DUPLICATE_STATUS.WARNING ?  'warning' : 'danger',
                msgKey: result.messageKey,
                msgArgs: result.messageArgs
              });

            }
            this.validated = result.status !== mica.commons.PERSON_DUPLICATE_STATUS.ERROR;
          })
          .catch(error => console.error('Error', error));
      }
    }

    onSave() {
      this.$scope.$broadcast('schemaFormValidate');
      if (this.$scope.form.$valid) {
        switch (this.mode) {
          case MODE.NEW:
            this.PersonResource.create(this.ContactSerializationService.serialize(angular.copy(this.person))).$promise
              .then((person) => this.navigateOut(`/person/${person.id}`));
            break;
          case MODE.EDIT:
            this.PersonResource.update(this.ContactSerializationService.serialize(angular.copy(this.person))).$promise
              .then((person) => this.navigateOut(`/person/${person.id}`));
            break;
        }
      }
    }

    onDelete() {
      this.__onDelete(
        'persons.delete-dialog.title',
        null,
        'persons.delete-dialog.message',
        [this.__getFullname()],
        () => this.__deletePerson(this.person.id)
      );
     }

    onDeleteEntity(entityType, entity) {
      const entityTitle = this.EntityTitleService.translate(entityType);
      this.__onDelete(
        'persons.delete-entities-dialog.title',
        [entityTitle],
        'persons.delete-entities-dialog.message',
        [entity.acronym],
        () => this.__deleteEntity(entityType, entity)
      );
    }

    onEditEntity(entityType, entity) {
      const membership = this.person[`${entityType}Memberships`];
      const membershipEntities = membership.filter((item) => item.parentId === entity.id);
      if (membershipEntities) {
        this.__openEntityRolesModal(this.config.roles, membershipEntities, entityType, entity);
      }
    }

    addNetworks() {
      this.__openMembershipsModal(
        this.__getFullname(),
        this.config.roles,
        this.memberships.networks,
        'NetworksResource',
        'network'
      );
    }

    addStudies() {
      this.__openMembershipsModal(
        this.__getFullname(),
        this.config.roles,
        this.memberships.studies,
        'StudyStatesSearchResource',
        'study',
        'className:Study'
      );
    }

    addInitiatives() {
      this.__openMembershipsModal(
        this.__getFullname(),
        this.config.roles,
        this.memberships.initiatives,
        'StudyStatesSearchResource',
        'initiative',
        'className:HarmonizationStudy'
      );
    }
  }

  mica.persons
    .component('personView', {
      bindings: {
      },
      templateUrl: 'app/persons/views/person-view.html',
      controllerAs: '$ctrl',
      controller: [
        '$rootScope',
        '$scope',
        '$location',
        '$routeParams',
        '$filter',
        '$uibModal',
        '$q',
        'screenSize',
        'EntityMembershipService',
        'LocalizedSchemaFormService',
        'MicaConfigResource',
        'PersonResource',
        'ContactsSearchResource',
        'ContactSerializationService',
        'NOTIFICATION_EVENTS',
        'FormDirtyStateObserver',
        'EntityTitleService',
        'AlertService',
        PersonViewController,
      ]
    });

})();
