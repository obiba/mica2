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

  const COUNT_TO_FILTER = {
    'total': 'ALL',
    'published': 'PUBLISHED',
    'under_review': 'UNDER_REVIEW',
    'in_edition': 'IN_EDITION',
    'to_delete': 'TO_DELETE'
  };


  const ENTITY_LISTING_URL = {
    Study: (filter) => `#/individual-study?filter=${filter}`,
    HarmonizationStudy: (filter) => `#/harmonization-study?filter=${filter}`,
    StudyDataset: (filter) => `#/collected-dataset?filter=${filter}`,
    HarmonizationDataset: (filter) => `#/harmonized-dataset?filter=${filter}`,
    Network: (filter) => `#/network?filter=${filter}`,
    Project: (filter) => `#/project?filter=${filter}`
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

    __getSearchUrl(type, property) {
      let data = {value: property.value};
      let query;
      let searchType;

      switch (type) {
        case 'Study':
          query = `study(in(Mica_study.className,Study))`;
          searchType = 'published' === property.name ? 'studies' : 'variables';
          if ('totalWithVariable' === property.name) {
            searchType = 'studies';
            query = `variable(in(Mica_variable.variableType,(Collected))),${query}`;
          }
          break;

        case 'HarmonizationStudy':
          query = `study(in(Mica_study.className,HarmonizationStudy))`;
          searchType = 'published' === property.name ? 'studies' : 'variables';
          break;

        case 'StudyDataset':
          query = `study(in(Mica_study.className,Study))`;
          searchType = 'datasets';
          break;

        case 'HarmonizationDataset':
          query = `study(in(Mica_study.className,HarmonizationStudy))`;
          searchType = 'datasets';
          break;
        case 'Network':
          searchType = 'networks';
          break;
        case 'DatasetVariable':
          searchType = 'variables';
          break;
      }

      return Object.assign(data, {
        url: query ? `#/search?type=${searchType}&display=list&query=${query}` : `#/search?type=${searchType}&display=list`
      });
    }

    __getListingUrl(type, property) {
      let data = {value: property.value};
      if ((property.name in COUNT_TO_FILTER)) {
        switch (type) {
          case 'Study':
          case 'HarmonizationStudy':
          case 'StudyDataset':
          case 'HarmonizationDataset':
          case 'Network':
          case 'DatasetVariable':
          case 'Project':
            data = Object.assign(data, {url: ENTITY_LISTING_URL[type](COUNT_TO_FILTER[property.name])});
        }
      }

      return data;
    }

    __createPropertyValue(type, property) {
      if ('Project' === type) {
        return this.__getListingUrl(type, property);
      }

      if (['published','variables','totalWithVariable'].indexOf(property.name) > -1) {
        return this.__getSearchUrl(type, property);
      }

      return this.__getListingUrl(type, property);
    }

    __processProperties(document) {
      // convert to map
      document.properties = document.properties.reduce((acc, property) => {
        acc[property.name] = property.value > 0 ? this.__createPropertyValue(document.type, property) : {value: property.value};
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
      if (!config.isNetworkEnabled) {
        delete sortOrder.Network;
      }
      if (!config.isCollectedDatasetEnabled) {
        delete sortOrder.StudyDataset;
      }
      if (!config.isHarmonizedDatasetEnabled) {
        delete sortOrder.HarmonizationDataset;
        delete sortOrder.HarmonizationStudy;
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
          if (stats.documents) {
            this.__processDocuments(stats, config);
          }
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
