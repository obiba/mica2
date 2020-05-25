/*
 * Copyright (c) 2020 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

(function () {

  class ContentStatsItem {
    constructor(MicaMetricsResource) {
    }

    $onChanges(changes) {
      console.debug(`Changes ${changes}`);
    }
  }


  mica.contentStats
    .component('contentStatsItem', {
      bindings: {
        document: '<'
      },
      templateUrl: 'app/content-stats/views/content-stats-item.html',
      controllerAs: '$ctrl',
      controller: [
        ContentStatsItem
      ]
    });

})();
