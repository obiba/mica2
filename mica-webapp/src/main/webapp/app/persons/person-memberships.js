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

  class PersonMembershipsController {

    constructor() {
      this.hasMembership = false;
      this.currentPage = 1;
      this.limit = DEFAULT_LIMIT;
    }

    deleteEntity(entity) {
      this.onDeleteEntity({entityType: this.entityType, entity});
    }

    editEntity(entity) {
      this.onEditEntity({entityType: this.entityType, entity});
    }

    $onChanges() {
      if (this.memberships) {
        this.hasMembership = this.memberships.entities && this.memberships.entities.length > 0;
        this.total = this.memberships.entities.length;
      }
    }

    onPageChanged(newPage, oldPage) {
      this.currentPage = newPage;
    }
  }

  mica.persons
    .component('personMemberships', {
      bindings: {
        memberships: '<',
        entityType: '@',
        roles: '<',
        onEditEntity: '&',
        onDeleteEntity: '&'
      },
      templateUrl: 'app/persons/views/person-memberships.html',
      controllerAs: '$ctrl',
      controller: [
        PersonMembershipsController
      ]
    });

})();
