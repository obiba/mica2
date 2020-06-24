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

  class EntityStateFilterDirective {

    constructor() {
      this.replace = true;
      this.bindToController = true;
      this.scope = {
        filter: '<',
        onSelected: '&'
      };
      this.templateUrl = 'app/commons/components/entity-state-filter/component.html',
      this.controllerAs = '$ctrl';
      this.controller = ['$filter', EntityStateFilterController];
    }
  }

  class EntityStateFilterController {
    constructor($filter) {
      this.$filter = $filter;
      this.filters = Object.values(mica.commons.ENTITY_STATE_FILTER)
        .map(value => ({
          label: this.$filter('translate')(`entity-state-filter.${value}`),
          value
        }));
      this._filter = mica.commons.ENTITY_STATE_FILTER.ALL;
      this.selected = null;
    }

    get filter() {
      return this._filter;
    }

    set filter(value) {
      if (value && !this.selected) {
        this._filter = value;
        const found = Object.keys(this.filters).filter(key => this.filters[key].value === this.filter);
        this.selected = this.filters[found] || mica.commons.ENTITY_STATE_FILTER.ALL;
      }
    }

    $onInit() {
      this.selected = this.selected || this.filters[0];
    }

    onChange(filter) {
      this.selected = filter;
      this.onSelected({filter: this.selected.value});
    }

  }

  mica.commons
    .directive('entityStateFilter', EntityStateFilterDirective);
})();
