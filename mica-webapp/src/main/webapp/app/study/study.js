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

mica.study = angular.module('mica.study', [
  'mica.config',
  'obiba.form',
  'obiba.mica.localized',
  'mica.fileSystem',
  'obiba.notification',
  'mica.commons',
  'mica.status',
  'mica.contact',
  'mica.permission',
  'mica.dataset',
  'ngResource',
  'ngRoute',
  'ui.bootstrap',
  'ui.select',
  'pascalprecht.translate',
  'obiba.mica.attachment',
  'mica.revisions',
  'mica.shareResource'
]);
