/*
 * Copyright (c) 2017 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

mica.attributes
  .directive('attributesList', [function () {
    return {
      restrict: 'E',
      replace: true,
      controller: 'AttributesListController',
      scope: {
        attributes: '='
      },
      templateUrl: 'app/attributes/views/attributes-list-template.html'
    };
  }])

  .directive('attributesEditableList', [function () {
    return {
      restrict: 'E',
      replace: true,
      controller: 'AttributesEditableListController',
      scope: {
        attributes: '='
      },
      templateUrl: 'app/attributes/views/attributes-editable-list-template.html'
    };
  }]);