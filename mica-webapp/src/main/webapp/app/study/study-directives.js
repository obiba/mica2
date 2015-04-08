'use strict';

mica.study
  .directive('numberOfParticipants', [function () {
    return {
      restrict: 'E',
      templateUrl: 'app/study/views/common/number-of-participants.html',
      scope: {
        numberOfParticipants: '=',
        lang: '='
      },
      link: function (scope) {
        scope.$watch('numberOfParticipants.sample.noLimit', function(value) {
          if (value) {
            delete scope.numberOfParticipants.sample.number;
          }
        }, true);

        scope.$watch('numberOfParticipants.participant.noLimit', function(value) {
          if (value) {
            delete scope.numberOfParticipants.participant.number;
          }
        }, true);
      }
    };
  }]);
