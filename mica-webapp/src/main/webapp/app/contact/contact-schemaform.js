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
var CONTACT_DEFINITION = [
  {
    type: 'section',
    items: [
      {
        type: 'fieldset',
        items: [
          'title',
          'firstName',
          'lastName',
          'academicLevel',
          'email',
          'phone'
        ]
      },
      {
        type: 'fieldset',
        items: [
          {
            type: 'help',
            helpvalue: '<h3>t(contact.institution)</h3>'
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
