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
      this.loading = false;
    }

    __processIndexedProperty(properties) {
      let published = properties.published;
      const notIndexed = published.value - properties.indexed.value;
      delete properties.indexed;

      if (notIndexed > 0) {
        published.errors = notIndexed;
      }
    }

    __processRequireIndexingProperty(properties) {
      let published = properties.published;
      const notIndexed = properties.requireIndexing.value;
      delete properties.requireIndexing;

      if (notIndexed > 0) {
        published.errors = notIndexed;
      }
    }

    __processProperties(document) {
      // convert to map
      document.properties = document.properties.reduce((acc, property) => {
        acc[property.name] = {value: property.value};
        return acc;
      }, {});

      if ('indexed' in document.properties) {
        this.__processIndexedProperty(document.properties);
      }

      if ('requireIndexing' in document.properties) {
        this.__processRequireIndexingProperty(document.properties);
      }
    }

    __processDocuments(stats) {
      stats.documents = stats.documents.filter((document) => SORT_ORDER.indexOf(document.type) > -1);
      stats.documents.sort((a, b) => {
        const ia = SORT_ORDER.indexOf(a.type);
        const ib = SORT_ORDER.indexOf(b.type);
        return ia - ib;
      });

      stats.documents.forEach(document => this.__processProperties(document));
    }

    __fetchData() {
      this.loading = true;
      this.MicaMetricsResource.get().$promise
        .then(stats => {
          this.loading = false;
          this.__processDocuments(stats);
          this.stats = stats;
        })
        .catch(this.loading = false);
    }

    $onInit() {
      this.__fetchData();
    }

    onRefresh() {
      this.__fetchData();
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
