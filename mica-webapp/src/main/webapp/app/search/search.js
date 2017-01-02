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

mica.search = angular.module('mica.search', [
    'obiba.mica.search'
  ])
  .config(['ngObibaMicaSearchProvider', 'ngObibaMicaSearchTemplateUrlProvider',
    function (ngObibaMicaSearchProvider, ngObibaMicaSearchTemplateUrlProvider) {
      ngObibaMicaSearchProvider.setLocaleResolver(['$q', '$translate', 'MicaConfigResource', function ($q, $translate, MicaConfigResource) {
        var res = $q.defer();

        MicaConfigResource.get(function (micaConfig) {
          res.resolve(micaConfig.languages || $translate.use());
        });

        return res.promise;
      }]);
      ngObibaMicaSearchProvider.setOptions({showSearchRefreshButton: true});
      ngObibaMicaSearchTemplateUrlProvider.setHeaderUrl('search', 'app/search/views/search-view-header.html');
      ngObibaMicaSearchTemplateUrlProvider.setHeaderUrl('classifications', 'app/search/views/classifications-view-header.html');
    }]);
