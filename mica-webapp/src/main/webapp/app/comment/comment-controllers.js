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

mica.comment.controller('CommentController', ['$scope', '$rootScope', '$routeParams', 'NOTIFICATION_EVENTS', 'CommentResource', 'CommentsResource', 'UserProfileService', function ($scope, $rootScope, $routeParams, NOTIFICATION_EVENTS, CommentResource, CommentsResource,UserProfileService) {
  $scope.comments = [];

  var onError = function (response) {
    $rootScope.$broadcast(NOTIFICATION_EVENTS.showNotificationDialog, {
      message: response.data ? response.data : angular.fromJson(response)
    });
  };

  $scope.retrieveComments = function () {
    $scope.comments = CommentsResource.query({type: $scope.type, id: $routeParams.id});
  };

  $scope.submitComment = function (comment) {
    CommentsResource.save({type: $scope.type, id: $routeParams.id}, comment.message, $scope.retrieveComments, onError);
  };

  $scope.updateComment = function (comment) {
    CommentResource.update({
      type: $scope.type,
      id: $routeParams.id,
      commentId: comment.id
    }, comment.message, $scope.retrieveComments, onError);
  };

  $scope.deleteComment = function (comment) {
    $scope.commentToDelete = comment.id;

    $rootScope.$broadcast(NOTIFICATION_EVENTS.showConfirmDialog,
      {
        titleKey: 'comment.delete-dialog.title',
        messageKey: 'comment.delete-dialog.message',
        messageArgs: [comment.createdBy]
      }, comment.id
    );
  };

  $scope.$on(NOTIFICATION_EVENTS.confirmDialogAccepted, function (event, id) {
    if ($scope.commentToDelete === id) {
      CommentResource.delete({
        type: $scope.type,
        id: $routeParams.id,
        commentId: id
      }, {}, $scope.retrieveComments, onError);
    }
  });

  $scope.getFullName = function(profile) {
    return UserProfileService.getFullName(profile);
  };

  $scope.retrieveComments();
}]);
