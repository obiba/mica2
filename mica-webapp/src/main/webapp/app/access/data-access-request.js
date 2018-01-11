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

mica.dataAccessRequest = angular.module('mica.data-access-request', [
  'obiba.mica.access'
])
  .config(['ngObibaMicaAccessTemplateUrlProvider',
    function (ngObibaMicaAccessTemplateUrlProvider) {
      ngObibaMicaAccessTemplateUrlProvider.setHeaderUrl('view','app/access/views/data-access-request-view-header.html');
      ngObibaMicaAccessTemplateUrlProvider.setHeaderUrl('form','app/access/views/data-access-request-form-header.html');
      ngObibaMicaAccessTemplateUrlProvider.setHeaderUrl('list','app/access/views/data-access-request-list-header.html');
    }]);


