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

mica.sets = angular.module('mica.sets', [
  'obiba.mica.sets'
]);

mica.sets
  .config(['ngObibaMicaSetsTemplateUrlProvider',
    function (ngObibaMicaSetsTemplateUrlProvider) {
      ngObibaMicaSetsTemplateUrlProvider.setHeaderUrl('cart', 'app/sets/views/cart-view-header.html');
    }]);
