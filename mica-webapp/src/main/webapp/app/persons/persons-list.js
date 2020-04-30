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

  const DEFAULT_LIMIT = 20;

  class PersonsListController {
    constructor($timeout, ContactsSearchResource) {
      this.$timeout = $timeout;
      this.ContactsSearchResource = ContactsSearchResource;
      this.loading = false;
      this.limit = DEFAULT_LIMIT;
      this.persons = [];
      this.total = 0;
      this._query = null;
      this.timeoutHandler = null;
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

      this.timeoutHandler = this.$timeout((this.getPersons(this._query + '*', 0)), 500);
    }

    getPersons(query, from, limit) {
      this.loading = true;
      this.ContactsSearchResource.get({
        query: query,
        from: from,
        limit: limit || DEFAULT_LIMIT
      }).$promise.then(result => {
        this.loading = false;
        this.persons = result.persons || [];
        this.total = result.total;
      });
    }

    $onInit() {
      this.getPersons(null, 0);
    }

    onPageChanged(newPage/*, oldPage*/) {
      const from = DEFAULT_LIMIT * (newPage - 1);
      this.getPersons(null, from);
    }
  }

  mica.persons
    .component('personsList', {
      bindings: {
      },
      templateUrl: 'app/persons/views/persons-list.html',
      controllerAs: '$ctrl',
      controller: ['$timeout', 'ContactsSearchResource', PersonsListController]
    });

})();
