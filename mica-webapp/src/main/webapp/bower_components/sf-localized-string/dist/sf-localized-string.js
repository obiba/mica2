angular.module("templates", []).run(["$templateCache", function($templateCache) {$templateCache.put("src/templates/sf-localized-string.html","<div class=\"form-group\" ng-class=\"{\'has-error\': hasError()}\" schema-validate=\"form\" sf-field-model >\n  <!--<pre>{{form|json}}</pre>-->\n  <label class=\"control-label\" ng-show=\"showTitle()\">{{form.title}}</label>\n  <div ng-if=\"form.schema.format === \'localizedString\'\"\n       ng-class=\"{\'form-group\' : !$last, \'input-group\' : form.showLocales || form.locales.length > 1}\"\n       ng-repeat=\"locale in form.locales\">\n    <span class=\"input-group-addon\"\n          ng-if=\"form.showLocales || form.locales.length > 1\">{{locale}}</span>\n    <input type=\"text\" class=\"form-control\"\n           sf-field-model=\"replaceAll\" ng-model=\"$$value$$[locale]\"></input>\n  </div>\n  <div ng-if=\"form.schema.format === \'localizedTextArea\'\"\n       ng-class=\"{\'form-group\' : !$last, \'input-group\' : form.showLocales || form.locales.length > 1}\"\n       ng-repeat=\"locale in form.locales\">\n    <span class=\"input-group-addon\"\n          ng-if=\"form.showLocales || form.locales.length > 1\">{{locale}}</span>\n    <textarea class=\"form-control\"\n              sf-field-model=\"replaceAll\" ng-model=\"$$value$$[locale]\"\n              rows=\"{{form.rows ? form.rows : 5}}\"></textarea>\n  </div>\n  <span class=\"help-block\" sf-message=\"form.description\"></span>\n</div>\n");}]);
angular.module('sfLocalizedString', [
  'schemaForm',
  'templates'
]).config(['schemaFormProvider', 'schemaFormDecoratorsProvider', 'sfBuilderProvider', 'sfPathProvider',
  function(schemaFormProvider,  schemaFormDecoratorsProvider, sfBuilderProvider, sfPathProvider) {

  var locStr = function(name, schema, options) {
    if (schema.type === 'object' && (schema.format == 'localizedString' || schema.format == 'localizedTextArea')) {
      var f = schemaFormProvider.stdFormObj(name, schema, options);
      f.key  = options.path;
      f.type = 'localizedstring';
      if(!f.locales) {
        f.locales = ['en'];
      }
      // f.validationMessage = {
      //   completed: 'All localized fields must be completed'
      // };
      // f.$validators = {
      //   completed: function(value) {
      //     if (value && Object.keys(value).length !== 0) {
      //       var count = f.locales.map(function(locale) {
      //         return value.hasOwnProperty(locale) ? 1 : 0;
      //       }).reduce(function (previous, current) {
      //         return previous + current;
      //       });
      //       console.log(f.locales.length === count);
      //       return f.locales.length === count;
      //     }
      //     return true;
      //   }
      // };
      options.lookup[sfPathProvider.stringify(options.path)] = f;
      return f;
    }
  };

  schemaFormProvider.defaults.object.unshift(locStr);
  
  schemaFormDecoratorsProvider.defineAddOn(
    'bootstrapDecorator',           // Name of the decorator you want to add to.
    'localizedstring',                      // Form type that should render this add-on
    'src/templates/sf-localized-string.html',  // Template name in $templateCache
    sfBuilderProvider.stdBuilders   // List of builder functions to apply.
  );

}]);
