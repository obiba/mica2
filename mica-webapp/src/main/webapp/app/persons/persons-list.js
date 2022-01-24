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

  class PersonsListController {
    constructor($rootScope, $timeout, $location, ContactsSearchResource, EntityMembershipService) {
      this.onLocationChangeHandler = $rootScope.$on('$locationChangeSuccess', () => this.onLocationChange(arguments));
      this.$timeout = $timeout;
      this.$location = $location;
      this.ContactsSearchResource = ContactsSearchResource;
      this.EntityMembershipService = EntityMembershipService;
      this.loading = false;
      this.pagination = {page: 1, size: mica.commons.DEFAULT_LIMIT};
      this.persons = [];
      this.total = 0;
      this._query = null;
      this.timeoutHandler = null;
      this.ngObibaStringUtils = new obiba.utils.NgObibaStringUtils();
    }

    get query() {
      return this._query || null;
    }

    set query(text) {
      this._query = text ? text.trim() : null;

      if (this.timeoutHandler) {
        this.$timeout.cancel(this.timeoutHandler);
      }

      this.timeoutHandler = this.$timeout((this.__getPersons(this._query, 0)), 250);
    }

    __getDownloadUrl() {
      let url = `ws/draft/persons/_search/_download?limit=${this.total}`;
      if (this.query) {
        url = `${url}&query=${mica.commons.cleanupQuery(this.query)}`;
      }

      return url;
    }

    __getPersons(query, from, limit, exclude) {
      const searchQuery = query ? mica.commons.cleanupQuery(query) : query;
      this.loading = true;
      this.ContactsSearchResource.search({
        query: searchQuery,
        from: from,
        limit: limit || mica.commons.DEFAULT_LIMIT,
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
          this.downloadUrl = this.__getDownloadUrl();
        })
        .catch(error => {
          console.error(`Search failed for ${searchQuery} - ${error.data ? error.data.message : error.statusText}`);
          this.loading = false;
          this.persons = [];
          this.total = 0;
        });
    }

    __getPaginationFromUrl(search) {
      let pagination = {page: 1, size: mica.commons.DEFAULT_LIMIT};

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
      search.size = pagination.size;
      this.$location.search(search);
    }

    __calculateFromByPage(page) {
      return this.pagination.size * (page - 1);
    }

    __setFocusOnSearchInput() {
      const searchInput = document.querySelectorAll('#persons-listing #persons-search-input');
      if (searchInput) {
        searchInput[0].focus();
      }
    }

    __fetchPersons(exclude) {
      const from = this.__calculateFromByPage(this.pagination.page);
      this.__getPersons(this.query, from, this.pagination.size, exclude);
    }

    __getValidPage(pagination, userDeleted) {
      const total = 'total' in pagination ? parseInt(pagination.total) : this.total;
      const from = this.__calculateFromByPage(pagination.page);

      if (userDeleted) {
        if (total - 1 === from) {
          // last item was deleted, go to previous page
          return Math.max(1, pagination.page - 1);
        }
      } else if (from >= total) {
        return 1;
      }

      return pagination.page;
    }

    navigateOut(path) {
      let pagination = this.pagination;
      if (pagination) {
        pagination.total = this.total;
        pagination.query = this.query;
      }

      this.$location.path(path).search(pagination || {});
    }

    $onInit() {
      this.__setFocusOnSearchInput();
      const search = this.$location.search();
      this.pagination = this.__getPaginationFromUrl(search);

      if (search && 'total' in search) {
        // Exclude the deleted user before retrieving users, there may be a delay in indexing persons and if the exclusion
        // is not made, the deleted user will be included in the new list
        this.exclude = search.exclude;
        this.total = search.total;
        this._query = search.query || '';
        search.page = this.__getValidPage(this.pagination, 'exclude' in search);
        delete search.exclude;
        delete search.total;
        delete search.query;
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
        const search = this.$location.search();
        this.pagination = this.__getPaginationFromUrl(search);
        const validPage = this.__getValidPage(this.pagination);
        if (validPage !== this.pagination.page) {
          // correct URL
          this.pagination.page = validPage;
          this.$location.search(this.pagination).replace();
        } else {
          const exclude = this.exclude;
          this.exclude = null;
          this.__fetchPersons(exclude);
          this.__setFocusOnSearchInput();
        }
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
