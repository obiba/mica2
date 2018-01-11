/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

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
  StatesResource,
  DraftDeleteService,
  AlertBuilder) {

  var self = this;
  var currentSearch = null;
  self.totalCount = 0;
  self.limit = 20;
  self.documents = [];
  self.loading = true;
  self.pagination = {
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

  self.hasDocuments = function () {
    return $scope.totalCount && angular.isDefined($scope.pagination.searchText);
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

  function onSuccess(response, responseHeaders) {
    self.searching = angular.isDefined($scope.pagination) && '' !== $scope.pagination.searchText;
    self.documents = response;
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
    var data = {from:(page - 1) * self.limit, limit: self.limit};

    if (self.pagination.searchText) {
      data.query = self.pagination.searchText + '*';
    }
    StatesResource.query(data, onSuccess, AlertBuilder.newBuilder().onError(onError));
  }

  loadPage(self.pagination.current);
};
