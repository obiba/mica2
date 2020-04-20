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

  class PersonMembershipsController {
    static DEFAULT_LIMIT = 20;

    constructor($filter,
                $translate,
                LocalizedValues) {
      this.$filter = $filter;
      this.$translate = $translate;
      this.LocalizedValues = LocalizedValues;
    }

    __initMembershipTableData(type, memberships) {
      const lang = this.$translate.use();
      const data = {
        title: this.$filter('translate')(type),
        entity: {}
      };

      let entityMap = data.entity;

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

    $onChanges() {
      if (this.person) {
        this.__initMembershipsTableData();
      }
    }
  }

  mica.persons
    .component('personMemberships', {
      bindings: {
        person: "<"
      },
      templateUrl: 'app/persons/views/person-memberships.html',
      controllerAs: '$ctrl',
      controller: [
        '$filter',
        '$translate',
        'LocalizedValues',
        PersonMembershipsController
      ]
    });

})();
