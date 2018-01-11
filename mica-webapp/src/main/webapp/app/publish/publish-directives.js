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

mica.publish
  .directive('publishStatus', [function () {
    return {
      restrict: 'AE',
      replace: true,
      scope: {
        state: '='
      },
      templateUrl: 'app/publish/publish-status-template.html'
    };
  }])

  .directive('publishSwitch', [function () {
    return {
      restrict: 'AE',
      replace: true,
      scope: {
        status: '&',
        publish: '&'
      },
      templateUrl: 'app/publish/publish-switch-template.html'
    };
  }]);
