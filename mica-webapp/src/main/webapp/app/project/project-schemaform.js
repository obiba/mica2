
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
