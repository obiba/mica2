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
  class MembershipsTableAlertHeaderController {

    constructor() {
      this._selectionsCount = 0;
    }

    get selectionsCount() {
      return this._selectionsCount;
    }

    set selectionsCount(value) {
      this._selectionsCount = value;
      if (value < 2) {
        this.entitiesType = this.entityType;
      } else {
        this.entitiesType = this.entityType === 'network' ? 'networks' : 'studies';
      }
    }

    $onChanges() {
    }
  }

  mica.persons
    .component('membershipsTableAlertHeader', {
      bindings: {
        allSelected: '<',
        selectionsCount: '<',
        entityType: '<',
        roles: '<',
        onSelectAll: '&',
        onDeleteSelections: '&'
      },
      templateUrl: 'app/persons/views/memberships-table-alert-header.html',
      controllerAs: '$ctrl',
      controller: [
        MembershipsTableAlertHeaderController
      ]
    });

})();
