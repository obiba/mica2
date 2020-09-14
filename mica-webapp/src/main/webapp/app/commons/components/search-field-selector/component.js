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

  class SearchFieldSelector {

    constructor() {
      this.replace = true;
      this.bindToController = true;
      this.scope = {
        fields: '<',
        field: '<',
        onSelected: '&'
      };
      this.templateUrl = 'app/commons/components/search-field-selector/component.html';
      this.controllerAs = '$ctrl';
      this.controller = ['$rootScope', '$filter', '$translate', SearchFieldSelectorController];
    }
  }

  class SearchFieldSelectorController {
    constructor($rootScope, $filter, $translate) {
      this.$rootScope = $rootScope;
      this.$filter = $filter;
      this.$translate = $translate;
      this.selected = null;
      this.__onLocaleChangedHandler = $rootScope.$on('$translateChangeSuccess', () => {
        this.__initializeFilters();
        this.__findAndSetSelectedFilter(this.selected ? this.selected.value : null);
      });
    }

    __initializeFilters() {
      this.fieldsData = (this.fields || [])
        .map(data => ({
          label: this.$filter('translate')(data.trKey),
          value: data
        }));
    }

    __findAndSetSelectedFilter(field) {
      const index = this.fields.indexOf(this.field);
      this.selected = this.fieldsData[index] || this.fieldsData[0];
    }

    $onChanges(changesObj) {
      if (changesObj.fields.currentValue) {
        this.__initializeFilters();
        this.__findAndSetSelectedFilter(this.field)
      }
    }

    $onDestroy() {
      this.__onLocaleChangedHandler();
    }

    onChange(field) {
      this.selected = field;
      this.onSelected({field: this.selected.value});
    }
  }

  mica.commons
    .directive('searchFieldSelector', SearchFieldSelector);
})();
