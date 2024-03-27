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

mica.dataset

  .directive('studyTableModalFormSection', [function() {
    return {
      restrict: 'EA',
      replace: true,
      scope: false,
      templateUrl: 'app/dataset/views/study-table-modal-form-section.html'
    };
  }])
  .directive('datasetStudyTablesForm', [function() {
  return {
    restrict: 'EA',
    replace: true,
    scope: false,
    templateUrl: 'app/dataset/views/harmonization-study-tables-form.html'
  };
}]);

(function () {
  class DatasetsListByStudyComponent {
    constructor($q, $timeout, CollectedDatasetsResource, HarmonizedDatasetsResource, CollectedDatasetPublicationResource, HarmonizedDatasetPublicationResource) {
      this.$q = $q;
      this.$timeout = $timeout;
      this.CollectedDatasetsResource = CollectedDatasetsResource;
      this.HarmonizedDatasetsResource = HarmonizedDatasetsResource;
      this.CollectedDatasetPublicationResource = CollectedDatasetPublicationResource;
      this.HarmonizedDatasetPublicationResource = HarmonizedDatasetPublicationResource;

      this.datasets = [];
      this.selection = [];
      this.canPublishAtLeastOneDataset = false;
      this.requestSent = false;
    }

    toggleDatasetSelection(datasetId) {
      let index = this.selection.indexOf(datasetId);
      if (index > -1) {
        this.selection.splice(index, 1);
      } else {
        this.selection.push(datasetId);
      }
    }

    onToggleAllDatasetsSelection() {
      if (this.selection.length === this.datasets.length) {
        this.selection = [];
      } else {
        this.selection = this.datasets.map(dataset => dataset.id);
      }
    }

    onClose() {
      this.selection = [];
    }

    indexOrPublish() {
      if (this.selection.length > 0) {
        let forIndex = [];
        let forPublish = [];
        let publishingResource = (this.type === 'Harmonized' ? this.HarmonizedDatasetPublicationResource : this.CollectedDatasetPublicationResource);
        let indexingResource = (this.type === 'Harmonized' ? this.HarmonizedDatasetsResource : this.CollectedDatasetsResource);

        this.datasets.filter(dataset => this.selection.indexOf(dataset.id) > -1).forEach(dataset => {
          let state = dataset.state;

          if (state.publishedTag && state.revisionsAhead === 0) {
            forIndex.push(dataset.id);
          } else if (state.revisionStatus === 'DRAFT') {
            forPublish.push(dataset.id);
          }
        });

        let promises = [];

        if (forIndex.length > 0) {
          promises.push(indexingResource.index({id: forIndex}).$promise);
        }

        forPublish.forEach(item => promises.push(publishingResource.publish({id: item}).$promise));

        this.$q.all(promises).then(() => {
          this.$timeout(() => {
            this.requestsSent = false;
          }, 3000);
        });

        this.selection = [];
        this.requestsSent = true;
      }
    }

    $onChanges(changeObj) {
      if (changeObj.study && changeObj.study.currentValue) {
        (this.type === 'Harmonized' ? this.HarmonizedDatasetsResource : this.CollectedDatasetsResource).query({'study': this.study}).$promise.then(data => {
          if (data) {
            this.datasets = data.filter(item => item.permissions.publish);
            this.canPublishAtLeastOneDataset = this.datasets.length > 0;
          }

          return data;
        });

      }
    }
  }

  mica.dataset.component('datasetsListByStudy', {
      bindings: {
        study: '<',
        type: '<'
      },
      templateUrl: 'app/dataset/views/datasets-list-by-study-component.html',
      controllerAs: '$ctrl',
      controller: [
        '$q',
        '$timeout',
        'CollectedDatasetsResource',
        'HarmonizedDatasetsResource',
        'CollectedDatasetPublicationResource',
        'HarmonizedDatasetPublicationResource',
        DatasetsListByStudyComponent
      ]
    });
})();
