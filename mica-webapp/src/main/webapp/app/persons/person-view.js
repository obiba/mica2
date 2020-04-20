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

  class PersonViewController {

    static DEFAULT_LIMIT = 20;

    constructor($routeParams,
                $filter,
                LocalizedSchemaFormService,
                MicaConfigResource,
                PersonResource,
                ContactSerializationService) {
      this.$routeParams = $routeParams;
      this.$filter = $filter;
      this.LocalizedSchemaFormService = LocalizedSchemaFormService;
      this.MicaConfigResource = MicaConfigResource;
      this.PersonResource = PersonResource;
      this.ContactSerializationService = ContactSerializationService;
    }

    $onInit() {
      this.MicaConfigResource.get().$promise.then(config => {
        this.config = config;
        const languages = this.config.languages.reduce((map, locale) => {
          map[locale] = this.$filter('translate')('language.' + locale);
          return map;
        }, {});
        this.sfOptions = {formDefaults: {languages: languages, readonly: true}};
        this.sfForm = {
          schema: this.LocalizedSchemaFormService.translate(angular.copy(CONTACT_SCHEMA)),
          definition: this.LocalizedSchemaFormService.translate(angular.copy(CONTACT_DEFINITION))
        };

        this.PersonResource.get({id: this.$routeParams.id}).$promise.then(person => {
          this.person = this.ContactSerializationService.deserialize(person);
          console.debug(`Person ${person.id}`)
        });
      });

    }
  }

  mica.persons
    .component('personView', {
      bindings: {
      },
      templateUrl: 'app/persons/views/person-view.html',
      controllerAs: '$ctrl',
      controller: [
        '$routeParams',
        '$filter',
        'LocalizedSchemaFormService',
        'MicaConfigResource',
        'PersonResource',
        'ContactSerializationService',
        PersonViewController
      ]
    });

})();
