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
      this.properties = {};
    }


    __postProcessIndexedProperty() {
      let published = this.properties.published;
      const notIndexed = published.value - this.properties.indexed.value;
      delete this.properties.indexed;

      if (notIndexed > 0) {
        published.errors = notIndexed;
        published.tooltip = `${notIndexed} published studies are not indexed`;
      }
    }

    __postProcessProperties() {
      if ('indexed' in this.properties) {
        this.__postProcessIndexedProperty();
      }
    }

    $onInit() {
      if (this.document) {
        this.properties = this.document.properties.reduce((acc, property) => {
          acc[property.name] = {value: property.value};
          return acc;
        }, {});

        this.__postProcessProperties();
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
        EntityStatisticsSummaryItem
      ]
    });

})();
