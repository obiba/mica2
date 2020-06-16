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

  const SORT_ORDER = {
    'Study': true,
    'HarmonizationStudy': true,
    'StudyDataset': true,
    'HarmonizationDataset': true,
    'Network': true,
    'DatasetVariable': true,
    'Project': true,
    'DataAccessRequest': true
  };

  class EntityStatisticsSummaryView {
    constructor($q, MicaConfigResource, MicaMetricsResource) {
      this.$q = $q;
      this.MicaConfigResource = MicaConfigResource;
      this.MicaMetricsResource = MicaMetricsResource;
      this.loading = false;
      this.errors = false;
    }

    __processIndexedProperty(properties) {
      let published = properties.published;
      const notIndexed = published.value - properties.indexed.value;
      delete properties.indexed;

      if (notIndexed > 0) {
        published.errors = notIndexed;
        this.errors = true;
      }
    }

    __processRequireIndexingProperty(properties) {
      let published = properties.published;
      const notIndexed = properties.requireIndexing.value;
      delete properties.requireIndexing;

      if (notIndexed > 0) {
        published.errors = notIndexed;
        this.errors = true;
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

    __processDocuments(stats, config) {
      let sortOrder = Object.assign({}, SORT_ORDER);
      if (!config.isVariablesCountEnabled) {
        delete sortOrder.DatasetVariable;
      }
      if (!config.isProjectsCountEnabled) {
        delete sortOrder.Project;
      }
      if (!config.isDataAccessRequestsCountEnabled) {
        delete sortOrder.DataAccessRequest;
      }
      sortOrder = Object.keys(sortOrder);

      stats.documents = stats.documents.filter((document) => sortOrder.indexOf(document.type) > -1);
      stats.documents.sort((a, b) => {
        const ia = sortOrder.indexOf(a.type);
        const ib = sortOrder.indexOf(b.type);
        return ia - ib;
      });

      stats.documents.forEach(document => this.__processProperties(document));
    }

    __fetchData() {
      this.errors = false;
      this.loading = true;
      this.$q
        .all([this.MicaConfigResource.get().$promise, this.MicaMetricsResource.get().$promise])
        .then(results => {
          const config = results[0];
          const stats = results[1];
          this.loading = false;
          this.__processDocuments(stats, config);
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
      '$q',
      'MicaConfigResource',
      'MicaMetricsResource',
      EntityStatisticsSummaryView
    ]
  });

})();
