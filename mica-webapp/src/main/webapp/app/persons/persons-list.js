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
    constructor($rootScope, $timeout, $location, ContactsSearchResource, EntityMembershipService) {
      this.onLocationChangeHandler = $rootScope.$on('$locationChangeSuccess', () => this.onLocationChange(arguments));
      this.$timeout = $timeout;
      this.$location = $location;
      this.ContactsSearchResource = ContactsSearchResource;
      this.EntityMembershipService = EntityMembershipService;
      this.loading = false;
      this.pagination = {page: 1, size: DEFAULT_LIMIT};
      this.persons = [];
      this.total = 0;
      this._query = null;
      this.timeoutHandler = null;
      this.ngObibaStringUtils = new obiba.utils.NgObibaStringUtils();
    }

    __cleanupQuery(text) {
      const cleaners = [
        this.ngObibaStringUtils.cleanOrEscapeSpecialLuceneBrackets,
        this.ngObibaStringUtils.cleanDoubleQuotesLeftUnclosed,
        (text) => text.replace(/[!^~\\/]/g,''),
        (text) => text.match(/\*$/) === null ? `${text}*` : text,
      ];

      let cleaned = text;
      cleaners.forEach(cleaner => cleaned = cleaner.apply(null, [cleaned.trim()]));

      return cleaned && cleaned.length > 1 ? cleaned : null;
    }

    get query() {
      return this._query || null;
    }

    set query(text) {
      this._query = text ? text.trim() : null;

      if (this._query === 1) {
        return;
      }

      if (this.timeoutHandler) {
        this.$timeout.cancel(this.timeoutHandler);
      }

      this.timeoutHandler = this.$timeout((this.getPersons(this._query, 0)), 250);
    }

    getPersons(query, from, limit, exclude) {
      const searchQuery = query ? this.__cleanupQuery(query) : query;
      this.loading = true;
      this.ContactsSearchResource.search({
        query: searchQuery,
        from: from,
        limit: limit || DEFAULT_LIMIT,
        exclude: exclude
      }).$promise
        .then(result => {
          this.loading = false;
          this.persons = (result.persons || []).map((person) => {
            if (person.networkMemberships) {
              person.networks = this.EntityMembershipService.groupRolesByEntity('networks', person.networkMemberships);
            }

            if (person.studyMemberships) {
              person.studies = this.EntityMembershipService.groupRolesByEntity('studies', person.studyMemberships);
            }

            return person;
          });
          this.total = result.total;
        })
        .catch(error => {
          console.error(`Search failed for ${searchQuery} - ${error.data ? error.data.message : error.statusText}`);
          this.loading = false;
          this.persons = [];
          this.total = 0;
        });
    }

    __getPaginationFromUrl(search) {
      let pagination = {page: 1, size: DEFAULT_LIMIT}

      if (search) {
        if ('page' in search) {
          pagination.page = parseInt(search.page);
        }

        if ('size' in search) {
          pagination.size = parseInt(search.size);
        }
      }

      return pagination;
    }

    __setPaginationInUrl(pagination) {
      const search = this.$location.search();
      search.page = pagination.page;
      search.size = this.pagination.size;
      this.$location.search(search);
    }

    __calculateFromByPage(page) {
      return DEFAULT_LIMIT * (page - 1);
    }

    __setFocusOnSearchInput() {
      const searchInput = document.querySelectorAll("#persons-listing #persons-search-input");
      if (searchInput) {
        searchInput[0].focus();
      }
    }

    __fetchPersons(exclude) {
      const from = this.__calculateFromByPage(this.pagination.page);
      this.getPersons(this.query, from, this.pagination.size, exclude);
    }

    navigateOut(path) {
      this.$location.path(path).search(this.pagination || {});
    }

    $onInit() {
      this.__setFocusOnSearchInput();
      const search = this.$location.search();
      this.pagination = this.__getPaginationFromUrl(search);

      if (search && 'exclude' in search) {
        // Exclude the deleted user before retrieving users, there may be a delay in indexing persons and if the exclusion
        // is not made, the deleted user will be included in the new list
        this.exclude = search.exclude;
        delete search.exclude;
        // Location change will fetch users
        this.$location.search(search).replace();
      } else {
        this.__fetchPersons();
      }
    }

    $onDestroy() {
      this.onLocationChangeHandler();
    }

    onPageChanged(newPage/*, oldPage*/) {
      this.pagination.page = newPage;
      this.__setPaginationInUrl(this.pagination);
    }

    onPageSizeSelected(size) {
      this.pagination.size = size;
      this.pagination.page = 1;
      this.__setPaginationInUrl(this.pagination, true);
    }

    onLocationChange() {
      const path = this.$location.path();
      if (path.startsWith('/persons')) {
        this.pagination = this.__getPaginationFromUrl(this.$location.search());
        const exclude = this.exclude;
        this.exclude = null;
        this.__fetchPersons(exclude);
        this.__setFocusOnSearchInput();
      }
    }
  }

  mica.persons
    .component('personsList', {
      bindings: {
      },
      templateUrl: 'app/persons/views/persons-list.html',
      controllerAs: '$ctrl',
      controller: [
        '$rootScope',
        '$timeout',
        '$location',
        'ContactsSearchResource',
        'EntityMembershipService',
        PersonsListController
      ]
    });

})();
