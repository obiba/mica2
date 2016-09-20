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

mica.dataset

  .directive('studyTableModalFormSection', [function() {
    return {
      restrict: 'EA',
      replace: true,
      scope: false,
      templateUrl: 'app/dataset/views/study-table-modal-form-section.html'
    };
  }])
  .directive('networkTableModalFormSection', [function() {
    return {
      restrict: 'EA',
      replace: true,
      scope: false,
      templateUrl: 'app/dataset/views/network-table-modal-form-section.html'
    };
  }])
  .directive('datasetStudyTablesView', [function() {
    return {
      restrict: 'EA',
      replace: true,
      scope: false,
      templateUrl: 'app/dataset/views/harmonization-study-tables-view.html'
    };
  }])
  .directive('datasetStudyTablesForm', [function() {
  return {
    restrict: 'EA',
    replace: true,
    scope: false,
    templateUrl: 'app/dataset/views/harmonization-study-tables-form.html'
  };
}]);
