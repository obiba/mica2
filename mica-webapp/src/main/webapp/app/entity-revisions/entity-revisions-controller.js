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

mica.revisions
  .controller('RevisionsController', [
    '$rootScope',
    '$scope',
    '$uibModal',
    function ($rootScope, $scope, $uibModal) {
      var onSuccess = function (revisions) {
        $scope.commitInfos = revisions;
        viewRevision($scope.active.index, $scope.id, $scope.commitInfos[$scope.active.index]);
      };

      var viewRevision = function (index, id, commitInfo) {
        $scope.active.realIndex = $scope.commitInfos.indexOf(commitInfo);
        $scope.active.index = index;
        $scope.active.page = $scope.pages.index;
        $scope.onViewRevision()(id, commitInfo);
      };

      var restoreRevision = function (id, commitInfo) {
        $scope.onRestoreRevision()(id, commitInfo, function () {
          $scope.onFetchRevisions()($scope.id, onSuccess);
        });
      };

      var viewDiff = function (id, leftSideCommitInfo, rightSideCommitInfo, comparedWithPrevious) {
        const response = $scope.onViewDiff()(id, leftSideCommitInfo, rightSideCommitInfo);

        response.$promise.then(function (data) {
          const diffIsEmpty = Object.keys(data.onlyLeft).length === 0 && Object.keys(data.differing).length === 0 && Object.keys(data.onlyRight).length === 0;
          const totalNumberOfFields = Object.keys(data.onlyLeft).length + Object.keys(data.differing).length + Object.keys(data.onlyRight).length;

          $uibModal.open({
            windowClass: 'entity-revision-diff-modal',
            templateUrl: 'app/entity-revisions/entity-revisions-diff-modal-template.html',
            controller: ['$scope', '$uibModalInstance',
            function($scope, $uibModalInstance) {
              $scope.checkedFields = [];
              $scope.diff = data;
              $scope.diffIsEmpty = diffIsEmpty;

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
                let foundIndex = -1;
                let index = 0;

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

              $scope.comparedWithPrevious = comparedWithPrevious;
              $scope.currentCommit = leftSideCommitInfo;
              $scope.commitInfo = rightSideCommitInfo;
            }],
            size: 'lg'
          }).result.then(function (chosenFields) {
            if (chosenFields && chosenFields.length > 0 && chosenFields.length < totalNumberOfFields) {
              $scope.onRestoreFromFields()(function (entity) {
                console.log('args', chosenFields, entity);

                const res = createObjectFromChosenFields(chosenFields, angular.copy(entity));

                console.log('final', res);
                return res;
              });
            } else {
              // restoreRevision(id, rightSideCommitInfo);
            }
          });
        });
      };

      function addFromChosenFields(splitName, value, arrayPathRegxp, entity) {
        let acc = entity;

        splitName.forEach(function (cur, idx) {
          const found = cur.match(arrayPathRegxp);
          const trueCur = found === null ? cur : cur.replace(arrayPathRegxp, '$1');

          if (idx === (splitName.length - 1)) {
            if (found === null) {
              acc[trueCur] = value;
            } else {
              if (Array.isArray(acc[trueCur])) {
                if (acc[trueCur].indexOf(value) === -1) {
                  acc[trueCur].push(value);
                }
              } else {
                acc[trueCur] = [value];
              }
            }
          } else {
            let newObject = {};

            if (found === null) {
              if (acc[trueCur]) {
                newObject = acc[trueCur];
              } else {
                acc[trueCur] = newObject;
              }

              acc = newObject;
            } else {
              if (Array.isArray(acc[trueCur])) {
                acc[trueCur].push(newObject);
              } else {
                acc[trueCur] = [newObject];
              }

              acc = newObject;
            }
          }
        });

        return entity;
      }

      function removeFromChosenFields(splitName, arrayPathRegxp, entity) {
        let acc = entity;

        splitName.forEach(function (cur, idx) {
          const found = cur.match(arrayPathRegxp);
          const trueCur = found === null ? cur : cur.replace(arrayPathRegxp, '$1');

          if (idx === (splitName.length - 1)) {
            if (found === null) {
              delete acc[trueCur];
            } else {
              if (Array.isArray(acc[trueCur])) {
                const index = cur.replace(arrayPathRegxp, '$3');
                if (index) {
                  const parsedIndex = Number.parseInt(index);
                  acc[trueCur].splice(parsedIndex, 1);
                }
              } else {
                delete acc[trueCur];
              }
            }
          } else {
            if (found === null) {
              acc = acc[trueCur];
            } else {
              const index = cur.replace(arrayPathRegxp, '$3');
              if (index) {
                const parsedIndex = Number.parseInt(index);
                acc = acc[trueCur][parsedIndex];
              }

              acc = acc[trueCur];
            }
          }
        });

        return entity;
      }

      function createObjectFromChosenFields(chosenFields, entity) {
        const arrayPathRegxp = /^(\w+)(\[(\d+)])$/;
        const objectFromChosenFields = entity || {};

        chosenFields.forEach(function (current) {
          const splitName = (current.name || current).split('.');
          if (typeof current === 'string') {
            console.log('remove', current, removeFromChosenFields(splitName, arrayPathRegxp, objectFromChosenFields));
          } else {
            console.log('add', current, addFromChosenFields(splitName, current.value, arrayPathRegxp, objectFromChosenFields));
          }
        });

        return objectFromChosenFields;
      }

      var onWatchId = function () {
        if ($scope.id) {
          $scope.onFetchRevisions()($scope.id, onSuccess);
        }
      };

      var canPaginate = function () {
        return $scope.commitInfos && $scope.commitInfos.length > $scope.pages.perPage;
      };

      $scope.pages = {index: 1, perPage: 5};
      $scope.active = {index: 0, realIndex: 0, page: 1};
      $scope.$watch('id', onWatchId);
      $scope.viewRevision = viewRevision;
      $scope.restoreRevision = restoreRevision;
      $scope.canPaginate = canPaginate;
      $scope.viewDiff = viewDiff;
    }]);
