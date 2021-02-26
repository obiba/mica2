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

/* exported CONTACT_SCHEMA */
var CONTACT_SCHEMA = {
  type: 'object',
  properties: {
    title: {
      title: 't(contact.title)',
      type: 'string'
    },
    firstName: {
      title: 't(contact.firstName)',
      type: 'string'
    },
    lastName: {
      title: 't(contact.lastName)',
      type: 'string'
    },
    academicLevel: {
      title: 't(contact.academicLevel)',
      type: 'string'
    },
    email: {
      title: 't(contact.email)',
      type: 'string',
      pattern: '^\\S+@\\S+$'
    },
    phone: {
      title: 't(contact.phone)',
      type: 'string'
    },
    institution: {
      type: 'object',
      properties: {
        name: {
          title: 't(name)',
          type: 'object',
          format: 'localizedString'
        },
        department: {
          title: 't(contact.department)',
          type: 'object',
          format: 'localizedString'
        },
        address: {
          type: 'object',
          properties: {
            street: {
              title: 't(address.label)',
              type: 'object',
              format: 'localizedString'
            },
            city: {
              title: 't(address.city)',
              type: 'object',
              format: 'localizedString'
            },
            zip: {
              title: 't(address.zip)',
              type: 'string'
            },
            state: {
              title: 't(address.state)',
              type: 'string'
            },
            country: {
              type: 'string',
              format: 'obibaCountriesUiSelect',
              title: 't(address.country)'
            }
          }
        }
      }
    }
  },
  required: [
    'lastName'
  ]
};

/* exported CONTACT_DEFINITION */
function CONTACT_DEFINITION(changeHandler) {
  var handler = changeHandler || function() {};

  return [
    {
      'type': 'section',
      'htmlClass': 'row',
      'items': [
        {
          'type': 'section',
          'htmlClass': 'col-xs-6',
          'items': [
            {
              type: 'help',
              helpvalue: '<h4>t(contact.identification)</h4>'
            },
            'title',
            {
              'key': 'firstName',
              'onChange': handler
            },
            {
              'key': 'lastName',
              'onChange': handler
            },
            'academicLevel',
            {
              'key': 'email',
              'onChange': handler
            },
            'phone'
          ]
        },
        {
          'type': 'section',
          'htmlClass': 'col-xs-6',
          'items': [
            {
              type: 'help',
              helpvalue: '<h4>t(contact.institution)</h4>'
            },
            {
              key: 'institution.name',
              type: 'localizedstring'
            },
            {
              key: 'institution.department',
              type: 'localizedstring'
            },
            {
              key: 'institution.address.street',
              type: 'localizedstring'
            },
            {
              key: 'institution.address.city',
              type: 'localizedstring'
            },
            'institution.address.zip',
            'institution.address.state',
            {
              key: 'institution.address.country',
              type: 'obibaCountriesUiSelect'
            }
          ]
        }
      ]
    }
  ];
}


