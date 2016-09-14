angular.module('schemaForm-datepicker', ['schemaForm', 'mgcrea.ngStrap.datepicker']).config(
['schemaFormProvider', 'schemaFormDecoratorsProvider', 'sfPathProvider', '$provide',
  function(schemaFormProvider,  schemaFormDecoratorsProvider, sfPathProvider, $provide) {

    var picker = function(name, schema, options) {
    if ((schema.type === 'string' || schema.type === 'number') && schema.format == 'datepicker') {
      var f = schemaFormProvider.stdFormObj(name, schema, options);
      f.key  = options.path;
      f.type = 'datepicker';
      options.lookup[sfPathProvider.stringify(options.path)] = f;
      return f;
    }
  };

    schemaFormProvider.defaults.string.unshift(picker);

  //Add to the bootstrap directive
    schemaFormDecoratorsProvider.addMapping('bootstrapDecorator', 'datepicker',
    'directives/decorators/bootstrap/strap/datepicker.html');
    schemaFormDecoratorsProvider.createDirective('datepicker',
    'directives/decorators/bootstrap/strap/datepicker.html');
  }])
    .filter('sfDatePickerDefaultFormat', ['moment', function (moment) {
      return function (dateStr, format) {
        format = format || '';
        return dateStr ? moment(dateStr, format.toUpperCase()).format('YYYY-MM-DD') : '';
      };
    }]);
