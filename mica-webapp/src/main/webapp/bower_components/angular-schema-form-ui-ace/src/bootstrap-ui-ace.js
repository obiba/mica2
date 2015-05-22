angular.module('schemaForm').config(
['schemaFormProvider', 'schemaFormDecoratorsProvider', 'sfPathProvider',
  function(schemaFormProvider,  schemaFormDecoratorsProvider, sfPathProvider) {

    var ace = function(name, schema, options) {
    if (schema.type === 'ace') {
      var f = schemaFormProvider.stdFormObj(name, schema, options);
      f.key  = options.path;
      f.type = 'ace';
      options.lookup[sfPathProvider.stringify(options.path)] = f;
      return f;
    }
  };

    schemaFormProvider.defaults.string.unshift(ace);

  //Add to the bootstrap directive
    schemaFormDecoratorsProvider.addMapping('bootstrapDecorator', 'ace',
    'directives/decorators/bootstrap/ace/ace.html');
    schemaFormDecoratorsProvider.createDirective('ace',
    'directives/decorators/bootstrap/ace/ace.html');
  }]);
