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

mica.search = angular.module('mica.search', [
    'obiba.mica.search'
  ]);

mica.search
    .config(['ngObibaMicaSearchTemplateUrlProvider',
    function (ngObibaMicaSearchTemplateUrlProvider) {
      ngObibaMicaSearchTemplateUrlProvider.setHeaderUrl('search', 'app/search/views/search-view-header.html');
      ngObibaMicaSearchTemplateUrlProvider.setHeaderUrl('classifications', 'app/search/views/classifications-view-header.html');
    }]);
