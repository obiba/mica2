/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

angular.module('obiba.mica.access')
  .directive('printFriendlyView', [function(){
    return {
      restrict: 'EA',
      replace: true,
      scope: false,
      templateUrl: 'access/views/data-access-request-print-preview.html'
    };
  }]);