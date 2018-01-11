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

mica.status
  .directive('statusButtons', [function () {
    return {
      restrict: 'AE',
      replace: true,
      scope: {
        state: '=',
        onEdit: '&',
        onPublish: '&',
        onUnPublish: '&',
        onDelete: '&',
        toDraft: '&',
        toUnderReview: '&',
        toDeleted: '&'
      },
      templateUrl: 'app/status/status-buttons-template.html'
    };
  }])

  .directive('fileStatusButtons', [function () {
    return {
      restrict: 'AE',
      replace: true,
      scope: {
        document: '=',
        documents: '=',
        onEdit: '&',
        onPublish: '&',
        onUnPublish: '&',
        onDelete: '&',
        toDraft: '&',
        toUnderReview: '&',
        toDeleted: '&'
      },
      templateUrl: 'app/status/file-status-buttons-template.html',
      link: function (scope) {
        function init(documents) {
          scope.canDelete = documents.filter(function (f) {
            return f.permissions.delete && f.revisionStatus === 'DELETED';
          }).length;

          scope.canPublish = documents.filter(function (f) {
            return f.permissions.publish && f.revisionStatus === 'UNDER_REVIEW';
          }).length;

          scope.canUnpublish = documents.filter(function (f) {
            return f.permissions.publish && f.state.publicationDate !== undefined;
          }).length;

          scope.canToDeleted = documents.filter(function (f) {
            return f.revisionStatus !== 'DELETED';
          }).length;

          scope.canToUnderReview = documents.filter(function (f) {
            return f.revisionStatus === 'DRAFT' && f.state.attachment.id !== f.state.publishedId;
          }).length;

          scope.canToDraft = documents.filter(function (f) {
            return f.revisionStatus !== 'DRAFT';
          }).length;
        }

        function initSingleDocument() {
          if (!scope.document) {
            return;
          }

          init([scope.document]);
          scope.isPublishDisabled = scope.document.state.attachment.id === scope.document.state.publishedId;
        }

        scope.$watch('document', function () {
          if (!scope.documents || scope.documents.length === 0) {
            initSingleDocument();
          }
        }, true);

        scope.$watch('documents', function () {
          if (!scope.documents || scope.documents.length === 0) {
            initSingleDocument();
            return;
          }

          init(scope.documents);
          scope.isPublishDisabled = false;
        }, true);
      }
    };
  }]);
