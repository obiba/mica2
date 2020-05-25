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
    'Project',
    'DatasetVariable',
  ];

  class ContentStatsView {
    constructor(MicaMetricsResource) {
      this.MicaMetricsResource = MicaMetricsResource;
    }

    $onInit() {
      this.MicaMetricsResource.get().$promise.then((stats) => {
        stats.documents.sort((a, b) => {
          const ia = SORT_ORDER.indexOf(a.type);
          const ib = SORT_ORDER.indexOf(b.type);
          return ia - ib;
        });
        this.stats = stats;
      });
    }

  }


  mica.contentStats
  .component('contentStatsView', {
    bindings: {
    },
    templateUrl: 'app/content-stats/views/content-stats-view.html',
    controllerAs: '$ctrl',
    controller: [
      'MicaMetricsResource',
      ContentStatsView
    ]
  });

})();
