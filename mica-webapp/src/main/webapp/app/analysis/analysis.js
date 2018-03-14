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

mica.analysis = angular.module('mica.analysis', [
    'obiba.mica.analysis'
  ]);

mica.analysis
    .config(['ngObibaMicaAnalysisTemplateUrlProvider',
    function (ngObibaMicaAnalysisTemplateUrlProvider) {
      ngObibaMicaAnalysisTemplateUrlProvider.setHeaderUrl('entities', 'app/analysis/views/entities-count-view-header.html');
    }]);
