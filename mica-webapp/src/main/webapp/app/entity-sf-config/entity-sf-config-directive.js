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

mica.entitySfConfig

  .directive('entitySfConfig', [function(){
    return {
      restrict: 'EA',
      replace: true,
      controller: 'EntitySfConfigController',
      scope: {
        form: '=',
        dirtyObservable: '=',
        alertId: '@'
      },
      templateUrl: 'app/entity-sf-config/views/entity-sf-config-form.html'
    };
  }]);