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

  class EntityStatisticsSummaryItem {
    constructor() {
    }
  }

  mica.entityStatisticsSummary
    .component('entityStatisticsSummaryItem', {
      bindings: {
        document: '<'
      },
      templateUrl: 'app/entity-statistics-summary/views/entity-statistics-summary-item.html',
      controllerAs: '$ctrl',
      controller: [
        EntityStatisticsSummaryItem
      ]
    });

})();
