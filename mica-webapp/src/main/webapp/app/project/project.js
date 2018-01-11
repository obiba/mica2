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

mica.project = angular.module('mica.project', [
  'mica.config',
  'obiba.form',
  'mica.comment',
  'obiba.mica.localized',
  'obiba.mica.attachment',
  'mica.publish',
  'mica.status',
  'mica.commons',
  'obiba.notification',
  'ngResource',
  'ngRoute',
  'ui.bootstrap',
  'ui',
  'pascalprecht.translate',
  'mica.revisions',
  'mica.shareResource'
]);
