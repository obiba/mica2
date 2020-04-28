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
    NEW: 'new'
  };

  class PersonViewController {
    constructor($rootScope,
                $scope,
                $location,
                $routeParams,
                $filter,
                $translate,
                $uibModal,
                LocalizedValues,
                LocalizedSchemaFormService,
                MicaConfigResource,
                PersonResource,
                ContactSerializationService,
                NOTIFICATION_EVENTS,
                FormDirtyStateObserver,
                EntityTitleService) {
      this.$rootScope = $rootScope;
      this.$scope = $scope;
      this.$location = $location;
      this.$routeParams = $routeParams;
      this.$filter = $filter;
      this.$translate = $translate;
      this.$uibModal= $uibModal;
      this.LocalizedSchemaFormService = LocalizedSchemaFormService;
      this.LocalizedValues = LocalizedValues;
      this.MicaConfigResource = MicaConfigResource;
      this.PersonResource = PersonResource;
      this.ContactSerializationService = ContactSerializationService;
      this.NOTIFICATION_EVENTS = NOTIFICATION_EVENTS;
      this.FormDirtyStateObserver = FormDirtyStateObserver;
      this.EntityTitleService = EntityTitleService;
    }

    __getMode() {
      let mode = MODE.VIEW;
      const parts = this.$location.path().match(/\/(new|edit)$/);

      if (parts) {
        const modePart = parts[1];
        switch (modePart) {
          case MODE.NEW:
          case MODE.EDIT:
            mode = modePart;
            break;
        }
      }

      return mode;
    }

    __initMembershipTableData(type, memberships) {
      const lang = this.$translate.use();
      const data = {
        title: this.$filter('translate')(type),
        entities: []
      };

      let entityMap = {};

      memberships.forEach(membership => {
        let entity = entityMap[membership.parentId];
        if (!entity) {
          entity = entityMap[membership.parentId] = {};
          entity.id = membership.parentId;
          entity.acronym = this.LocalizedValues.forLang(membership.parentAcronym, lang);
          entity.name = this.LocalizedValues.forLang(membership.parentName, lang);
          entity.roles = [this.$filter('translate')(`contact.label.${membership.role}`)];
          if ('obiba.mica.PersonDto.StudyMembershipDto.meta' in membership) {
            entity.url = `/${membership['obiba.mica.PersonDto.StudyMembershipDto.meta'].type}/${entity.id}`;
          } else {
            entity.url = `/network/${entity.id}`;
          }
        } else {
          entity.roles = [].concat([this.$filter('translate')(`contact.label.${membership.role}`)], entity.roles);
        }
      });

      data.entities = Object.values(entityMap) || [];

      return data;
    }

    __initMembershipsTableData() {
      this.memberships = {};
      if (this.person.networkMemberships) {
        this.memberships.networks = this.__initMembershipTableData('networks', this.person.networkMemberships);
      }

      if (this.person.studyMemberships) {
        this.memberships.studies = this.__initMembershipTableData('studies', this.person.studyMemberships);
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
    __navigateOut(path) {
      this.__unobserveFormDirtyState();
      this.$location.path(path).replace();
    }

    __deletePerson(id) {
      this.PersonResource.delete({id}).$promise
        .then(() => {
          this.listenerRegistry.unregisterAll();
          this.__navigateOut('/persons');
        });
    }

    __updatePerson() {
      this.PersonResource.update(this.ContactSerializationService.serialize(this.person)).$promise
        .then((person) => {
          this.listenerRegistry.unregisterAll();
          this.__initPerson(person)
        });
    }

    __deleteEntities(entityType, entities) {
      let membership = this.person[`${entityType}Memberships`];
      membership = membership.filter((item) => entities.indexOf(item.parentId) === -1);
      if (membership.length === 0) {
        delete this.person[`${entityType}Memberships`];
      } else {
        this.person[`${entityType}Memberships`] = membership;
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

    __addMembership(roles, entities, entityType) {
      console.debug(JSON.stringify(this.person));

      let entityMemberhips = this.person[`${entityType}Memberships`] || [];
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
          entityMemberhips.push(membership);
        });
      });

      this.person[`${entityType}Memberships`] = entityMemberhips;
      this.__updatePerson();
    }

    __openModal(roles, membership, entitySearchResource, entityType) {
      const entityTitle = this.EntityTitleService.translate(entityType, true);
      this.$uibModal.open({
        templateUrl: 'app/persons/views/entity-list-modal.html',
        controllerAs: '$ctrl',
        controller: ['$uibModalInstance',
          function($uibModalInstance) {
            this.selectedRoles = [];
            this.selectedEntities = [];
            this.roles = roles;
            this.membership = membership || [];
            this.entitySearchResource = entitySearchResource;
            this.entityType = entityType;
            this.entityTitle = entityTitle;
            this.addDisabled = true;

            const updateAddDisable =
              () => this.addDisabled = this.selectedRoles.length < 1 || this.selectedEntities.length < 1;

            this.onRolesSelected = (selectedRoles) => {
              this.selectedRoles = selectedRoles;
              updateAddDisable.call(this);
            }
            this.onEntitiesSelected = (selectedEntities) => {
              this.selectedEntities = selectedEntities;
              updateAddDisable.call(this);
            }

            this.onAdd = () => $uibModalInstance.close(
              {
                roles: this.selectedRoles,
                entities: this.selectedEntities
              }
            );

            this.onClose = () => $uibModalInstance.dismiss('close');
          }]
      }).result.then(selections => {
        this.__addMembership(selections.roles, selections.entities, entityType);
      });
    }

    $onInit() {
      this.MicaConfigResource.get().$promise.then(config => {
        this.listenerRegistry = new obiba.utils.EventListenerRegistry();
        this.mode = this.__getMode();
        this.config = config;
        const languages = this.config.languages.reduce((map, locale) => {
          map[locale] = this.$filter('translate')('language.' + locale);
          return map;
        }, {});
        this.sfOptions = {formDefaults: {languages: languages, readonly: MODE.VIEW === this.mode}};
        this.sfForm = {
          schema: this.LocalizedSchemaFormService.translate(angular.copy(CONTACT_SCHEMA)),
          definition: this.LocalizedSchemaFormService.translate(angular.copy(CONTACT_DEFINITION))
        };

        if (MODE.NEW !== this.mode) {
          this.__getPerson(this.$routeParams.id);
        } else {
          this.person = {};
        }

        this.__observeFormDirtyState();
      });
    }

    onCancel() {
      switch (this.mode) {
        case MODE.NEW:
          this.__navigateOut('/persons');
          break;
        case MODE.EDIT:
          this.__navigateOut(`/person/${this.$routeParams.id}`);
          break;
      }
    }

    onSave() {
      this.$scope.$broadcast('schemaFormValidate');
      if (this.$scope.form.$valid) {
        switch (this.mode) {
          case MODE.NEW:
            this.PersonResource.create(this.person).$promise
              .then((person) => this.$location.path(`/person/${person.id}`).replace());
            break;
          case MODE.EDIT:
            this.PersonResource.update(this.ContactSerializationService.serialize(this.person)).$promise
              .then((person) => this.$location.path(`/person/${person.id}`).replace());
            break;
        }
      }
    }

    onDelete() {
      this.__onDelete(
        'persons.delete-dialog.title',
        null,
        'persons.delete-dialog.message',
        [`${this.person.firstName} ${this.person.lastName}`.trim()],
        () => this.__deletePerson(this.person.id)
      );
     }

    onDeleteEntities(entityType, entities) {
      const entityTitle = this.EntityTitleService.translate(entityType);
      const entitiesTitle = this.EntityTitleService.translate(entityType, entities.length > 1);
      this.__onDelete(
        'persons.delete-entities-dialog.title',
        [entityTitle],
        'persons.delete-entities-dialog.message',
        [entities.length, entitiesTitle],
        () => this.__deleteEntities(entityType, entities)
      );
    }

    addNetworks() {
      this.__openModal(this.config.roles, this.memberships.networks,'NetworksResource', 'network');
    }

    addStudies() {
      this.__openModal(this.config.roles, this.memberships.studies,'StudyStatesSearchResource', 'study');
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
        '$translate',
        '$uibModal',
        'LocalizedValues',
        'LocalizedSchemaFormService',
        'MicaConfigResource',
        'PersonResource',
        'ContactSerializationService',
        'NOTIFICATION_EVENTS',
        'FormDirtyStateObserver',
        'EntityTitleService',
        PersonViewController,

      ]
    });

})();
