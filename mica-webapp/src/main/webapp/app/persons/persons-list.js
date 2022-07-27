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

  function PersonsListController($rootScope, $timeout, $location, ContactsSearchResource, EntityMembershipService) {
    const self = this;

    self.onLocationChangeHandler = $rootScope.$on('$locationChangeSuccess', () => onLocationChange(arguments));
    self.$timeout = $timeout;
    self.$location = $location;
    self.ContactsSearchResource = ContactsSearchResource;
    self.EntityMembershipService = EntityMembershipService;
    self.loading = false;
    self.pagination = {page: 1, size: mica.commons.DEFAULT_LIMIT};
    self.persons = [];
    self.total = 0;
    self._query = null;
    self.timeoutHandler = null;
    self.ngObibaStringUtils = new obiba.utils.NgObibaStringUtils();
    self.sort = {column: 'lastName', order: 'asc'};

    Object.defineProperty(self, 'query', {
      enumerable: true,
      configurable: true,
      get () {
        return self._query || null;
      },
  
      set (text) {
        self._query = text ? text.trim() : null;
  
        if (self.timeoutHandler) {
          self.$timeout.cancel(self.timeoutHandler);
        }
  
        self.timeoutHandler = self.$timeout((getPersons(self._query, 0)), 250);
      }
    });

    function getDownloadUrl() {
      let url = `ws/draft/persons/_search/_download?limit=${self.total}`;
      if (self.query) {
        url = `${url}&query=${mica.commons.cleanupQuery(self.query)}`;
      }

      return url;
    }

    function getPersons(query, from, limit, exclude) {
      const searchQuery = query ? mica.commons.cleanupQuery(query) : query;
      self.loading = true;
      self.ContactsSearchResource.search({
        query: searchQuery,
        from: from,
        limit: limit || mica.commons.DEFAULT_LIMIT,
        sort: self.sort.column,
        order: self.sort.order,
        exclude: exclude
      }).$promise
        .then(result => {
          self.loading = false;
          self.persons = (result.persons || []).map((person) => {
            if (person.networkMemberships) {
              person.networks = self.EntityMembershipService.groupRolesByEntity('networks', person.networkMemberships);
            }

            if (person.studyMemberships) {
              person.studies =
                self.EntityMembershipService.groupRolesByEntity(
                  'studies',
                  person.studyMemberships.filter(membership => membership['obiba.mica.PersonDto.StudyMembershipDto.meta'].type ==='individual-study'));
            }

            if (person.studyMemberships) {
              person.initiatives =
                self.EntityMembershipService.groupRolesByEntity(
                  'initiatives',
                  person.studyMemberships.filter(membership => membership['obiba.mica.PersonDto.StudyMembershipDto.meta'].type !=='individual-study'));
            }

            return person;
          });
          self.total = result.total;
          self.downloadUrl = getDownloadUrl();
        })
        .catch(error => {
          console.error(`Search failed for ${searchQuery} - ${error.data ? error.data.message : error.statusText}`);
          self.loading = false;
          self.persons = [];
          self.total = 0;
        });
    }

    function getPaginationFromUrl(search) {
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

    function setPaginationInUrl(pagination) {
      const search = self.$location.search();
      search.page = pagination.page;
      search.size = pagination.size;
      self.$location.search(search);
    }

    function calculateFromByPage(page) {
      return self.pagination.size * (page - 1);
    }

    function setFocusOnSearchInput() {
      const searchInput = document.querySelectorAll('#persons-listing #persons-search-input');
      if (searchInput) {
        searchInput[0].focus();
      }
    }

    function fetchPersons(exclude) {
      const from = calculateFromByPage(self.pagination.page);
      getPersons(self.query, from, self.pagination.size, exclude);
    }

    function getValidPage(pagination, userDeleted) {
      const total = 'total' in pagination ? parseInt(pagination.total) : self.total;
      const from = calculateFromByPage(pagination.page);

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

    function navigateOut(path) { //
      let pagination = self.pagination;
      if (pagination) {
        pagination.total = self.total;
        pagination.query = self.query;
      }

      self.$location.path(path).search(pagination || {});
    }

    function onSortColumn(column, order) { //
      self.sort.column = column.replaceAll('__locale__', 'en') || 'lastName';
      self.sort.order = order || 'asc';

      if (self.timeoutHandler) {
        self.$timeout.cancel(self.timeoutHandler);
      }

      self.timeoutHandler = self.$timeout((getPersons(self._query, 0)), 250);
    }

    function $onInit() { //
      setFocusOnSearchInput();
      const search = self.$location.search();
      self.pagination = getPaginationFromUrl(search);

      if (search && 'total' in search) {
        // Exclude the deleted user before retrieving users, there may be a delay in indexing persons and if the exclusion
        // is not made, the deleted user will be included in the new list
        self.exclude = search.exclude;
        self.total = search.total;
        self._query = search.query || '';
        search.page = getValidPage(self.pagination, 'exclude' in search);
        delete search.exclude;
        delete search.total;
        delete search.query;
        // Location change will fetch users
        self.$location.search(search).replace();
      } else {
        fetchPersons();
      }
    }

    function $onDestroy() { //
      self.onLocationChangeHandler();
    }

    function onPageChanged(newPage/*, oldPage*/) { //
      self.pagination.page = newPage;
      setPaginationInUrl(self.pagination);
    }

    function onPageSizeSelected(size) { //
      self.pagination.size = size;
      self.pagination.page = 1;
      setPaginationInUrl(self.pagination, true);
    }

    function onLocationChange() { //
      const path = self.$location.path();
      if (path.startsWith('/persons')) {
        const search = self.$location.search();
        self.pagination = getPaginationFromUrl(search);
        const validPage = getValidPage(self.pagination);
        if (validPage !== self.pagination.page) {
          // correct URL
          self.pagination.page = validPage;
          self.$location.search(self.pagination).replace();
        } else {
          const exclude = self.exclude;
          self.exclude = null;
          fetchPersons(exclude);
          setFocusOnSearchInput();
        }
      }
    }

    self.onLocationChange = onLocationChange;
    self.onPageSizeSelected = onPageSizeSelected;
    self.onPageChanged = onPageChanged;
    self.$onDestroy = $onDestroy;
    self.$onInit = $onInit;
    self.onSortColumn = onSortColumn;
    self.navigateOut = navigateOut;
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
