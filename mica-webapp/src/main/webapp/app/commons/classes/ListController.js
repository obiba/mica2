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

  /**
   * Basic document list controller
   * Must not be overridden because of private function calls
   *
   * @param $scope
   * @param $timeout
   * @param StatesResource
   * @param DraftDeleteService
   * @param AlertBuilder
   * @constructor
   */
  mica.commons.ListController = function (
    $scope,
    $timeout,
    $translate,
    StatesResource,
    DraftDeleteService,
    AlertBuilder,
    EntityStateFilterService,
    MicaConfigResource) {

    const self = this;
    let currentSearch = null;
    const searchFields = [
      {field: 'id', trKey: 'id'},
      {field: 'acronym', trKey: 'acronym', localized: true},
      {field: 'name', trKey: 'name', localized: true},
      {field: 'all', trKey: 'all'},
    ];

    self.totalCount = 0;
    self.documents = [];
    self.loading = true;
    self.sort = {
      column: `lastModifiedDate`,
      order: 'desc'
    };
    self.search = {
      fields: searchFields,
      defaultField: searchFields[0],
      queryField: searchFields[0]
    };

    self.pagination = {
      size: mica.commons.DEFAULT_LIMIT,
      current: 1,
      get searchText() {
        return this._searchText || '';
      },
      set searchText(text) {
        if (currentSearch) {
          $timeout.cancel(currentSearch);
        }

        this._searchText = text;
        currentSearch = $timeout(function () {
          refreshPage();
        }, 500);
      }
    };

    self.onSortColumn = function(column, order) {
      self.sort.column = column.replaceAll('__locale__', getValidLocale()) || 'id';
      self.sort.order = order || 'asc';
      loadPage(self.pagination.current);
    };

    self.hasDocuments = function () {
      return $scope.totalCount && angular.isDefined($scope.pagination.searchText);
    };

    self.onPageSizeSelected = function(size) {
      self.pagination.size = size;
      self.pagination.current = 1;
      loadPage(self.pagination.current);
    };

    self.pageChanged = function (page, oldPage) {
      if (page !== oldPage) {
        loadPage(page);
      }
    };

    self.deleteDocument = function (doc) {
      DraftDeleteService.delete(doc, function () {
        refreshPage();
      });
    };

    self.onFilterSelected = function (filter) {
      EntityStateFilterService.updateUrl(filter);
      self.filter = filter;
      self.pagination.current = 1;
      loadPage(self.pagination.current);
    };

    self.onSearchFieldSelected = function (field) {
      console.debug(`Search field: ${field}`);
      let index = self.search.fields.indexOf(field);
      if (index > -1) {
        self.search.queryField = self.search.fields[index];
      } else {
        console.error('Invalid Field');
      }

      if (self.pagination.searchText) {
        loadPage(self.pagination.current);
      }
    };

    function onSuccess(response, responseHeaders) {
      self.searching = angular.isDefined($scope.pagination) && '' !== $scope.pagination.searchText;
      self.totalCount = parseInt(responseHeaders('X-Total-Count'), 10) || self.documents.length; // TODO remove last condition when harmo study is completed
      self.loading = false;

      angular.extend($scope, self); // update scope
    }

    function onError() {
      self.loading = false;
    }

    function refreshPage() {
      if (self.pagination.current !== 1) {
        self.pagination.current = 1;
      } else {
        loadPage(1);
      }
    }

    function loadPage(page) {
      const limit = self.pagination.size;
      let data = {
        from:(page - 1) * limit,
        limit: limit,
        filter: self.filter,
        sort: self.sort.column,
        order: self.sort.order
      };

      if (self.pagination.searchText) {
          data.query = mica.commons.addQueryFields(mica.commons.cleanupQuery(self.pagination.searchText), self.search.queryField, getValidLocale());
      }
      self.documents = StatesResource.query(data, onSuccess, AlertBuilder.newBuilder().onError(onError));
    }

    function getValidLocale() {
      const locale = $translate.use();
      return self.micaConfig.languages.indexOf(locale) > -1 ? locale : 'en';
    }

    function update() {
      self.filter = EntityStateFilterService.getFilterAndValidateUrl();
      loadPage(self.pagination.current);
    }

    $scope.$on('$locationChangeSuccess', () => update());

    MicaConfigResource.get().$promise.then((config) => {
      self.micaConfig = config;

      update();
    });

  };

})();
