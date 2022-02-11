/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/* exported OPAL_TABLE_SCHEMA*/
var OPAL_TABLE_SCHEMA = {
  type: 'object',
  properties: {
    name: {
      title: 't(dataset.table-name)',
      type: 'object',
      format: 'localizedString'
    },
    description: {
      title: 't(dataset.table-description)',
      type: 'object',
      format: 'localizedString'
    },
    additionalInformation: {
      title: 't(dataset.table-additional-information)',
      type: 'object',
      format: 'obibaSimpleMde'
    }
  }
};

/* exported OPAL_TABLE_DEFINITION */
var OPAL_TABLE_DEFINITION = [{
  type: 'fieldset',
  items: [
    {
      key: 'name',
      type: 'localizedstring'
    },
    {
      key: 'description',
      type: 'localizedstring',
      rows: 5
    },
    {
      "key": "additionalInformation",
      "type": "obibaSimpleMde",
      "rows": 2
    }
  ]
}];
