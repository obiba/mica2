
/* exported PROJECT_SCHEMA*/
var PROJECT_SCHEMA = {
  type: 'object',
  properties: {
    title: {
      title: 'Title',
      type: 'object',
      format: 'localizedString'
    },
    summary: {
      title: 'Summary',
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
      helpvalue: '<h3>General</h3>'
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
