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
