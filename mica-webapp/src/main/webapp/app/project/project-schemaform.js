
/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/* exported PROJECT_SCHEMA*/
var PROJECT_SCHEMA = {
  type: 'object',
  properties: {
    title: {
      title: 't(research-project.title)',
      type: 'object',
      format: 'localizedString'
    },
    summary: {
      title: 't(research-project.summary)',
      type: 'object',
      format: 'localizedString'
    }
  },
  required: ['title']
};

/* exported PROJECT_DEFINITION */
var PROJECT_DEFINITION = {
  type: 'fieldset',
  items: [
    {
      type: 'help',
      helpvalue: 't(research-project.main-section)'
    },
    {
      key: '_mica.title',
      type: 'localizedstring'
    },
    {
      key: '_mica.summary',
      type: 'localizedstring',
      rows: 5
    }
  ]
};
