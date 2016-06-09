
/* exported PROJECT_SCHEMA*/
var PROJECT_SCHEMA = {
  type: 'object',
  properties: {
    name: {
      title: 'Name',
      type: 'object',
      format: 'localizedString'
    },
    description: {
      title: 'Description',
      type: 'object',
      format: 'localizedString'
    }
  },
  required: ['name']
};

/* exported PROJECT_DEFINITION */
var PROJECT_DEFINITION = {
  type: 'fieldset',
  items: [
    {
      type: 'help',
      helpvalue: '<h3>General</h3>'
    },
    {
      key: '_mica.name',
      type: 'localizedstring'
    },
    {
      key: '_mica.description',
      type: 'localizedstring',
      rows: 5
    }
  ]
};
