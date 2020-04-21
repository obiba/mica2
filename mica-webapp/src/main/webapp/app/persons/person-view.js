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
                LocalizedValues,
                LocalizedSchemaFormService,
                MicaConfigResource,
                PersonResource,
                ContactSerializationService,
                NOTIFICATION_EVENTS) {
      this.$rootScope = $rootScope;
      this.$scope = $scope;
      this.$location = $location;
      this.$routeParams = $routeParams;
      this.$filter = $filter;
      this.$translate = $translate;
      this.LocalizedSchemaFormService = LocalizedSchemaFormService;
      this.LocalizedValues = LocalizedValues;
      this.MicaConfigResource = MicaConfigResource;
      this.PersonResource = PersonResource;
      this.ContactSerializationService = ContactSerializationService;
      this.NOTIFICATION_EVENTS = NOTIFICATION_EVENTS;
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

    __getPerson(id) {
      this.PersonResource.get({id}).$promise.then(person => {
        this.person = this.ContactSerializationService.deserialize(person);
        this.__initMembershipsTableData();
      });
    }

    __deletePerson(id) {
      this.PersonResource.delete({id}).$promise
        .then(() => {
          this.$location.path('/persons').replace()
          this.listenerRegistry.unregisterAll();
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

      });
    }

    onCancel() {
      switch (this.mode) {
        case MODE.NEW:
          this.$location.path('/persons').replace();
          break;
        case MODE.EDIT:
          this.$location.path(`/person/${this.$routeParams.id}`).replace();
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
      this.listenerRegistry.register(
        this.$scope.$on(this.NOTIFICATION_EVENTS.confirmDialogRejected, () => this.listenerRegistry.unregisterAll())
      );

      this.listenerRegistry.register(
        this.$scope.$on(this.NOTIFICATION_EVENTS.confirmDialogAccepted, () => this.__deletePerson(this.person.id))
      );

      this.$rootScope.$broadcast(this.NOTIFICATION_EVENTS.showConfirmDialog,
        {
          titleKey: 'persons.delete-dialog.title',
          messageKey: 'persons.delete-dialog.message',
          messageArgs: [`${this.person.firstName} ${this.person.lastName}`.trim()]
        }, {}
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
        '$translate',
        'LocalizedValues',
        'LocalizedSchemaFormService',
        'MicaConfigResource',
        'PersonResource',
        'ContactSerializationService',
        'NOTIFICATION_EVENTS',
        PersonViewController
      ]
    });

})();
