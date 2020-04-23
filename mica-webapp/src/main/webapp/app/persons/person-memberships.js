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

(function () {
  class PersonMembershipsController {

    constructor() {
      this.hasMembership = false;
    }

    $onChanges() {
      console.debug(`On Changes`);
      if (this.membership) {
        this.hasMembership = this.membership.entities && this.membership.entities.length > 0;
      }
    }

  }

  mica.persons
    .component('personMemberships', {
      bindings: {
        membership: '<',
        entityType: '@'
      },
      templateUrl: 'app/persons/views/person-memberships.html',
      controllerAs: '$ctrl',
      controller: [
        PersonMembershipsController
      ]
    });

})();
