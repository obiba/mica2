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

  class PersonEntityListController {
    constructor($injector, $translate, $timeout) {
      this.$injector = $injector;
      this.$translate = $translate;
      this.$timeout = $timeout;
    }

    __getEntities(query, from, limit) {
      this.loading = true;
      this.searchResource.query({query: query, from:from , limit: limit || DEFAULT_LIMIT}, (entities, headers) => {
        this.entities = entities;
        this.total = parseInt(headers('X-Total-Count'), 10) || entities.length;
        this.loading = false;
      });
    }

    get query() {
      return this._query || null;
    }

    set query(text) {
      this._query = text || null;

      if (text.length === 1) {
        return;
      }

      if (this.timeoutHandler) {
        this.$timeout.cancel(this.timeoutHandler);
      }

      this.timeoutHandler = this.$timeout((this.__getEntities(this._query, 0)), 500);
    }

    $onChanges() {
      if (this.roles && this.entitySearchResource && this.membership) {
        this.searchResource = this.$injector.get(this.entitySearchResource);
        if (!this.searchResource) {
          throw new Error(`Failed to inject resource ${this.entitySearchResource}`);
        }

        this.__getEntities(null, 0);
      }
    }

    $onInit() {
      this.language = this.$translate.use();
      this.limit = DEFAULT_LIMIT;
      this.selectedEntities = {};
      this.loading = false;
    }

    onPageChanged(newPage, oldPage) {
      console.debug(`PageChanged ${oldPage} ${newPage}`);
      const from = DEFAULT_LIMIT * (newPage - 1);
      this.__getEntities(null, from);
    }

    onSelected(selectedRoles) {
      this.onRolesSelected({selectedRoles: selectedRoles});
    }
  }

  mica.persons
    .component('personEntityList', {
      bindings: {
        roles: '<',
        membership: '<',
        entitySearchResource: '<',
        onRolesSelected: '&'
      },
      templateUrl: 'app/persons/views/entity-list.html',
      controllerAs: '$ctrl',
      controller: ['$injector', '$translate', '$timeout', PersonEntityListController]
    });

})();
