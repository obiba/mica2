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

mica.network = angular.module('mica.network', [
  'mica.config',
  'obiba.form',
  'mica.comment',
  'obiba.mica.localized',
  'obiba.mica.attachment',
  'mica.publish',
  'mica.status',
  'mica.commons',
  'mica.contact',
  'obiba.notification',
  'ngResource',
  'ngRoute',
  'ui.bootstrap',
  'pascalprecht.translate',
  'mica.revisions',
  'mica.shareResource'
]);
