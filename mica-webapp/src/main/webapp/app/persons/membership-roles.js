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
  class MembershipRolesController {
    constructor() {
    }

    $onChanges() {
      if (this.roles) {
        this.selections = this.roles.reduce((map, role) => {
          map[role] = false;
          return map;
        }, {});
      }

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
        entityType: '<',
        onSelected: '&'
      },
      templateUrl: 'app/persons/views/membership-roles.html',
      controllerAs: '$ctrl',
      controller: [MembershipRolesController]
    });

})();
