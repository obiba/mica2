'use strict';

mica.comment.controller('CommentController', ['$scope', '$rootScope', '$routeParams', 'NOTIFICATION_EVENTS', 'CommentResource', 'CommentsResource', function ($scope, $rootScope, $routeParams, NOTIFICATION_EVENTS, CommentResource, CommentsResource) {
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

  $scope.retrieveComments();
}]);
