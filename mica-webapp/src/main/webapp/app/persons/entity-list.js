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
    constructor($injector, $translate, $timeout, EntityTitleService) {
      this.$injector = $injector;
      this.$translate = $translate;
      this.$timeout = $timeout;
      this.EntityTitleService = EntityTitleService;
    }

    __getEntities(query, from, limit) {
      const excludes = (this.memberships.entities || []).map(entity => entity.id);
      this.loading = true;
      let searchQuery = query ? `${query}*` : query;
      if (this.filterQuery) {
        searchQuery = searchQuery ? `${searchQuery} AND ${this.filterQuery}` : this.filterQuery;
      }
      this.searchResource.query({query: searchQuery, from:from , limit: limit || DEFAULT_LIMIT, exclude: excludes  }, (entities, headers) => {
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

    $onInit() {
      this.entityTitle = this.EntityTitleService.translate(this.entityType, false);
      this.language = this.$translate.use();
      this.limit = DEFAULT_LIMIT;
      this.selectedEntities = {};
      this.selectedEntitiesData = {};
      this.loading = false;

      this.searchResource = this.$injector.get(this.entitySearchResource);
      if (!this.searchResource) {
        throw new Error(`Failed to inject resource ${this.entitySearchResource}`);
      }

      this.__getEntities(null, 0);
    }

    onPageChanged(newPage/*, oldPage*/) {
      const from = DEFAULT_LIMIT * (newPage - 1);
      this.__getEntities(null, from);
    }

    onSelectedRoles(selectedRoles) {
      this.onRolesSelected({selectedRoles: selectedRoles});
    }

    onEntitySelected(entity)
    {
      if (this.selectedEntities[entity.id]) {
        this.selectedEntitiesData[entity.id] = entity;
      } else {
        delete this.selectedEntitiesData[entity.id];
      }

      this.onEntitiesSelected(
        {
          selectedEntities: Object.values(this.selectedEntitiesData)
        }
      );
    }
  }

  mica.persons
    .component('personEntityList', {
      bindings: {
        roles: '<',
        memberships: '<',
        entitySearchResource: '<',
        entityType: '<',
        fullname: '<',
        filterQuery: '<',
        onRolesSelected: '&',
        onEntitiesSelected: '&'
      },
      templateUrl: 'app/persons/views/entity-list.html',
      controllerAs: '$ctrl',
      controller: ['$injector', '$translate', '$timeout', 'EntityTitleService', PersonEntityListController]
    });

})();
