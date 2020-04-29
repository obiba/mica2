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
  const DEFAULT_LIMIT = 10;

  class MembershipRolesController {
    constructor() {
      this.limit = DEFAULT_LIMIT;
    }

    $onInit() {
      this.selectedRoles = this.selectedRoles || [];
      this.selections = this.roles.reduce((map, role) => {
        map[role] = this.selectedRoles.indexOf(role) > -1;
        return map;
      }, {});
    }

    onRoleSelected() {
      const roles = Object.keys(this.selections).filter( selection => this.selections[selection]);
      this.onSelected({selectedRoles: roles});
    }

  }

  mica.persons
    .component('membershipRoles', {
      bindings: {
        roles: '<',
        selectedRoles: '<',
        entityType: '<',
        onSelected: '&'
      },
      templateUrl: 'app/persons/views/membership-roles.html',
      controllerAs: '$ctrl',
      controller: [MembershipRolesController]
    });

})();
