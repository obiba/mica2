angular.module("templates", []).run(["$templateCache", function($templateCache) {$templateCache.put("src/templates/sf-localized-string.html","<div class=\"form-group\">\n  <!--<pre>{{form|json}}</pre>-->\n  <label>{{form.title}}</label>\n  <div schema-validate=\"form\" sf-field-model>\n    <div ng-if=\"form.schema.format === \'localizedString\'\" class=\"input-group\" ng-repeat=\"locale in form.locales\" ng-class=\"{\'form-group\' : !$last}\">\n      <span class=\"input-group-addon\">{{locale}}</span>\n      <input type=\"text\" class=\"form-control\" sf-field-model=\"replaceAll\" ng-model=\"$$value$$[locale]\"></input>\n    </div>\n    <div ng-if=\"form.schema.format === \'localizedTextArea\'\" class=\"input-group\" ng-repeat=\"locale in form.locales\" ng-class=\"{\'form-group\' : !$last}\">\n      <span class=\"input-group-addon\">{{locale}}</span>\n      <textarea class=\"form-control\" sf-field-model=\"replaceAll\" ng-model=\"$$value$$[locale]\" rows=\"{{form.rows ? form.rows : 5}}\"></textarea>\n    </div>\n  </div>\n  <span class=\"help-block\" sf-message=\"form.description\"></span>\n</div>\n");}]);
angular.module('sfLocalizedString', [
  'schemaForm',
  'templates'
]).config(function(schemaFormProvider,  schemaFormDecoratorsProvider, sfBuilderProvider, sfPathProvider) {
  
  var locStr = function(name, schema, options) {
    if (schema.type === 'object' && (schema.format == 'localizedString' || schema.format == 'localizedTextArea')) {
      var f = schemaFormProvider.stdFormObj(name, schema, options);
      f.key  = options.path;
      f.type = 'localizedstring';
      if(!f.locales) {
        f.locales = ['en'];
      }
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

});
