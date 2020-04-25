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
  const DEFAULT_LIMIT = 2;

  class PersonMembershipsController {

    constructor() {
      this.hasMembership = false;
      this.pages = {count: 0};
      this.selections = {};
      this.currentPage = 1;
      this.limit = DEFAULT_LIMIT;
      this.selectedAllPages = false;
    }

    __updateSelectionsCount() {
      this.selectionsCount = Object.keys(this.selections || []).length;
    }

    __selectAllPage(page, value) {
      console.debug(`__selectAllPage ${page}`);
      this.pages[page].all = value;
      const entities = this.membership.entities;
      const start = (page - 1) * DEFAULT_LIMIT;
      const end = Math.min(start + DEFAULT_LIMIT, entities.length);
      if (value) {
        for (let i = start; i < end; i++) {
          this.selections[entities[i].id] = value;
        }
      } else {
        for (let i = start; i < end; i++) {
          delete this.selections[entities[i].id];
        }
      }

      this.__updateSelectionsCount();
    }

    get selectedAllPage() {
      return this.pages[this.currentPage] && this.pages[this.currentPage].all;
    }

    set selectedAllPage(value) {
      this.__selectAllPage(this.currentPage, value);
    }

    onSelection(id) {
      const selected = this.selections[id];

      if (!selected) {
        delete this.selections[id];
        if (this.pages[this.currentPage].all) {
          this.pages[this.currentPage].all = false;
        }
        this.selectedAllPages = false;
      } else {
        const entities = this.membership.entities;
        const start = (this.currentPage - 1) * DEFAULT_LIMIT;
        const end = Math.min(start + DEFAULT_LIMIT, entities.length);
        let allSelected = true;

        for (let i = start; i < end; i++) {
          allSelected = allSelected && this.selections[entities[i].id];
        }
        this.pages[this.currentPage].all = allSelected;
      }

      this.__updateSelectionsCount();
    }

    $onChanges() {
      console.debug(`On Changes`);
      if (this.membership) {
        this.hasMembership = this.membership.entities && this.membership.entities.length > 0;
        this.total = this.membership.entities.length;

        this.pages.count = Math.ceil(this.membership.entities.length / DEFAULT_LIMIT);
        for (let i = 0; i < this.pages.count; i++) {
          this.pages[i+1] = {all: false, selections: []};
        }
      }
    }

    onPageChanged(newPage, oldPage) {
      console.debug(`PageChanged ${oldPage} ${newPage}`);
      this.currentPage = newPage;
    }

    onSelectAll() {
      this.selectedAllPages = !this.selectedAllPages;
      for (let i = 0; i < this.pages.count; i++) {
        this.__selectAllPage(i+1, this.selectedAllPages);
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
