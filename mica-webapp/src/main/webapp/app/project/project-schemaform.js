
var PROJECT_SCHEMA = {
  type: 'object',
  properties: {
    name: {
      title: 'Name',
      type: 'string'
    },
    description: {
      title: 'Description',
      type: 'string'
    }
  }
};

var PROJECT_DEFINITION = {
  type: 'fieldset',
  items: [
    {
      type: 'help',
      helpvalue: '<h3>General</h3>'
    },
    '_mica.name',
    {
      key: '_mica.description',
      type: 'textarea'
    }
  ]
};
