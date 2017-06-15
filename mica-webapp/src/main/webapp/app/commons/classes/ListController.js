'use strict';

// can not override due to a call to loadPage
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
    return self.totalCount && !self.pagination.searchText;
  };

  self.pageChanged = function (page) {
    loadPage(page);
  };

  self.deleteDocument = function (doc) {
    DraftDeleteService.delete(doc, function () {
      refreshPage();
    });
  };

  function onSuccess(response, responseHeaders) {
    self.totalCount = parseInt(responseHeaders('X-Total-Count'), 10);
    self.documents = response;
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
