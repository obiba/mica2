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

  const SORT_ORDER = [
    'Study',
    'HarmonizationStudy',
    'StudyDataset',
    'HarmonizationDataset',
    'Network',
    'Project'
  ];

  class EntityStatisticsSummaryView {
    constructor(MicaMetricsResource) {
      this.MicaMetricsResource = MicaMetricsResource;
    }

    $onInit() {
      this.MicaMetricsResource.get().$promise.then((stats) => {
        stats.documents = stats.documents.filter(document => SORT_ORDER.indexOf(document.type) > -1);
        stats.documents.sort((a, b) => {
          const ia = SORT_ORDER.indexOf(a.type);
          const ib = SORT_ORDER.indexOf(b.type);
          return ia - ib;
        });

        this.stats = stats;
      });
    }

  }


  mica.entityStatisticsSummary
  .component('entityStatisticsSummaryView', {
    bindings: {
    },
    templateUrl: 'app/entity-statistics-summary/views/entity-statistics-summary-view.html',
    controllerAs: '$ctrl',
    controller: [
      'MicaMetricsResource',
      EntityStatisticsSummaryView
    ]
  });

})();
