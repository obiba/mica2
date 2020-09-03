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

  const DEFAULT_LIMIT = 20;
  const DEFAULT_SIZES = [10, 20, 50, 100];

  mica.commons.DEFAULT_SIZES = DEFAULT_SIZES;
  mica.commons.DEFAULT_LIMIT = DEFAULT_LIMIT;

  class PaginationSizeSelector {

    constructor() {
      this.pagination = {};
    }

    onChanged() {
      this.onSelected({size: this.pagination.selected.value});
    }

    $onChanges() {
      if (!this.sizes || this.sizes.length <1) {
        this.sizes = DEFAULT_SIZES;
      } else {
        // Sort in case the order is not correct
        this.sizes.sort((a,b) => a - b);
      }

      this.pagination.sizes = this.sizes.map(size => ({label: size.toString(), value: size}));

      if (!this.size) {
        this.size = DEFAULT_SIZES[1];
      }

      this.visible = !this.total || this.total > this.sizes[0];

      const index = this.sizes.indexOf(this.size);
      this.pagination.selected = this.pagination.sizes[index > -1 ? index : 1];
    }
  }

  mica.commons
    .component('paginationSizeSelector', {
      bindings: {
        total: '<',
        sizes: '<',
        size: '<',
        onSelected: '&'
      },
      templateUrl: 'app/commons/components/pagination/size-selector/component.html',
      controllerAs: '$ctrl',
      controller: [
        PaginationSizeSelector
      ]
    });

})();
