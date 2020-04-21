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
  // const DEFAULT_LIMIT = 20;

  class PersonMembershipsController {

    constructor() {
    }

    $onChanges() {
      if (this.membership) {
        console.debug(`Membership ${this.membership.title}`);
      }
    }
  }

  mica.persons
    .component('personMemberships', {
      bindings: {
        membership: '<'
      },
      templateUrl: 'app/persons/views/person-memberships.html',
      controllerAs: '$ctrl',
      controller: [
        PersonMembershipsController
      ]
    });

})();
