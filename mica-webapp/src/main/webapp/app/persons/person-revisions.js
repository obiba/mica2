'use strict';

(function () {
  const MODE = {
    VIEW: 'view',
    EDIT: 'edit',
    NEW: 'new',
    REVISIONS: 'revisions'
  };

  function getMode($location) {
    let mode = MODE.VIEW;
    const parts = $location.path().match(/\/(new|edit|revisions)$/);

    if (parts) {
      const modePart = parts[1];
      switch (modePart) {
        case MODE.NEW:
        case MODE.EDIT:
        case MODE.REVISIONS:
          mode = modePart;
          break;
      }
    }

    return mode;
  }

  function getScreenSize(screenSize) {
    var size = ['lg', 'md', 'sm', 'xs'].filter(function (size) {
      return screenSize.is(size);
    });

    return {
      size: size ? size[0] : 'lg',
      device: screenSize.is('md, lg') ? 'desktop' : 'mobile',
      is: screenSize.is
    };
  }

  class PersonRevisionsController {
    constructor($rootScope, $scope, $location, $routeParams, $filter, $translate, $uibModal, $q, screenSize, PersonResource, PersonRevisionsResource, PersonRestoreRevisionResource, PersonViewRevisionResource) {
      this.$rootScope = $rootScope;
      this.$scope = $scope;
      this.$location = $location;
      this.$routeParams = $routeParams;
      this.$filter = $filter;
      this.$translate = $translate;
      this.$uibModal = $uibModal;
      this.$q = $q;
      this.screenSize = screenSize;
      this.PersonResource = PersonResource;
      this.PersonRevisionsResource = PersonRevisionsResource;
      this.PersonRestoreRevisionResource = PersonRestoreRevisionResource;
      this.PersonViewRevisionResource = PersonViewRevisionResource;
    }

    viewDiff(leftSide, rightSide) {
      const ctrl = this;

      if (leftSide && rightSide) {
        this.PersonRevisionsResource.diff({id: this.id, left: leftSide.commitId, right: rightSide.commitId, locale: this.$translate.use()}).$promise.then(data => {

          const diffIsEmpty = Object.keys(data.onlyLeft).length === 0 && Object.keys(data.differing).length === 0 && Object.keys(data.onlyRight).length === 0;
          
          ctrl.$uibModal.open({
            windowClass: 'entity-revision-diff-modal',
            templateUrl: 'app/entity-revisions/entity-revisions-diff-modal-template.html',
            controller: ['$scope', '$uibModalInstance',
            function($scope, $uibModalInstance) {
              $scope.checkedFields = [];
              $scope.diff = data;
              $scope.diffIsEmpty = diffIsEmpty;

              $scope.noRestore = true;

              $scope.toggleCheckedField = function (field) {
                if (field) {
                  const fieldIndex = $scope.findField(typeof field === 'string' ? field : field.name);
                  if (fieldIndex > -1) {
                    $scope.checkedFields.splice(fieldIndex, 1);
                  } else {
                    $scope.checkedFields.push(field);
                  }
                }
              };

              $scope.findField = function (fieldName) {
                var foundIndex = -1;
                var index = 0;

                while (foundIndex === -1 && index < $scope.checkedFields.length) {
                  if ((typeof $scope.checkedFields[index] === 'string' && $scope.checkedFields[index] === fieldName) || $scope.checkedFields[index].name === fieldName) {
                    foundIndex = index;
                  } else {
                    index++;
                  }
                }

                return foundIndex;
              };

              $scope.cancel = function () {
                $uibModalInstance.dismiss();
              };

              $scope.restoreRevision = function () {
                $uibModalInstance.close($scope.checkedFields);
              };

              $scope.currentCommit = leftSide;
              $scope.commitInfo = rightSide;
            }],
            size: 'lg'
          });
        });
      }      
    }

    navigateOut(path, exclude) {
      var search = this.pagination || {};
      if (exclude) {
        search.exclude = exclude;
      }
      this.$location.path(path).search(search).replace();
    }

    $onInit() {
      const ctrl = this;
      this.mode = getMode(this.$location);
      this.layoutHelper = getScreenSize(this.screenSize);
      this.pagination = this.$location.search();

      this.id = this.$routeParams.id;
      this.$q.all([this.PersonRevisionsResource.get({id: this.id}).$promise, this.PersonResource.get({id: this.id}).$promise])
      .then(responses => {
        ctrl.commitInfos = responses[0];
        ctrl.person = responses[1];
      }, 
      reasons => {
        console.error(reasons);
      });
    }
  }

  mica.persons.component('personRevisions', {
    bindings: {},
    templateUrl: 'app/persons/views/person-revisions.html',
    controllerAs: '$ctrl',
    controller: ['$rootScope', '$scope', '$location', '$routeParams', '$filter', '$translate', '$uibModal', '$q', 'screenSize', 'PersonResource', 'PersonRevisionsResource', 'PersonRestoreRevisionResource', 'PersonViewRevisionResource', PersonRevisionsController]
  });

})();