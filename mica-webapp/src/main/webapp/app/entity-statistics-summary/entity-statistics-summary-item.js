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

  const ENTITY_RESOURCE_MAP = {
    'Study': 'individual-studies',
    'HarmonizationStudy': 'harmonization-studies',
    'StudyDataset': 'collected-datasets',
    'HarmonizationDataset': 'harmonized-datasets',
    'Network': 'networks',
    'Project': 'projects'
  };

  class EntityStatisticsSummaryItem {
    constructor($uibModal, $filter, $translate, EntityIndexHealthResource, DraftEntitiesIndexResource) {
      this.$uibModal = $uibModal;
      this.$filter = $filter;
      this.$translate = $translate;
      this.EntityIndexHealthResource = EntityIndexHealthResource;
      this.DraftEntitiesIndexResource = DraftEntitiesIndexResource;
    }

    __openIndexingModal(document, $filter, EntityIndexHealthResource, DraftEntitiesIndexResource) {
      const locale = this.$translate.use();

      this.$uibModal.open({
        templateUrl: 'app/entity-statistics-summary/views/entity-statistics-summary-indexing-modal.html',
        controllerAs: '$ctrl',
        controller: ['$uibModalInstance',
          function($uibModalInstance) {
            this.document = document;
            this.loading = true;
            this.entityTitle = $filter('translate')(`entity-statistics-summary.${document.type}`);
            EntityIndexHealthResource.get(
              {entityResource: ENTITY_RESOURCE_MAP[document.type]}).$promise
              .then(response => {
                this.loading = false;
                this.requireIndexing = response.requireIndexing
                  .concat()
                  .sort((a, b) => {
                    if (a.id < b.id) {
                      return -1;
                    } else if (a.id > b.id) {
                      return 1;
                    }

                    return 0;
                  });
              })
              .catch(response => {
                this.loading = false;
                console.debug(response);
              });

            this.onIndex = () => {
              const ids = this.requireIndexing.map(item => item.id);
              DraftEntitiesIndexResource.build({entityResource: ENTITY_RESOURCE_MAP[document.type], id: ids}).$promise
                .then($uibModalInstance.dismiss('close'))
                .catch(response => console.debug(response));
            };

            this.onClose = () => $uibModalInstance.dismiss('close');
          }]
      }).result.then();
    }

    onIndex() {
      try {
        this.__openIndexingModal(this.document, this.$filter, this.EntityIndexHealthResource, this.DraftEntitiesIndexResource);
      } catch (e) {
        console.debug(e);
      }
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
        '$uibModal',
        '$filter',
        '$translate',
        'EntityIndexHealthResource',
        'DraftEntitiesIndexResource',
        EntityStatisticsSummaryItem
      ]
    });

})();
