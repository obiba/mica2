angular.module("sfLocalizedStringTemplates", []).run(["$templateCache", function($templateCache) {$templateCache.put("src/templates/sf-localized-string.html","<div class=\"form-group\"\n     ng-controller=\"LocalizedStringController\"\n     ng-class=\"{\'has-error\': form.disableErrorState !== true && hasError(), \'has-success\': form.disableSuccessState !== true && hasSuccess(), \'has-feedback\': form.feedback !== false }\"\n     schema-validate=\"form\" sf-field-model >\n  <!--<pre>{{form|json}}</pre>-->\n  <label class=\"control-label\" ng-show=\"showTitle()\">{{form.title}}</label>\n  <div class=\"pull-right dropdown\" ng-class=\"{\'open\': open}\" ng-if=\"form.showLocales && form.locales && form.locales.length > 1\">\n    <a href class=\"dropdown-toggle badge\" ng-click=\"toggleDropdown()\">{{form.languages[selectedLocale]}} <span class=\"caret\"></span></a>\n    <ul class=\"dropdown-menu\">\n      <li ng-repeat=\"loc in form.locales\"><a href ng-click=\"selectLocale(loc)\">{{form.languages[loc]}}</a></li>\n    </ul>\n  </div>\n  <div ng-if=\"!form.rows || form.rows <= 1\"\n       ng-class=\"{\'form-group\' : !$last}\">\n    <input type=\"text\" class=\"form-control\"\n           ng-disabled=\"form.readonly\"\n           sf-field-model=\"replaceAll\" ng-model=\"$$value$$[locale]\"\n           ng-repeat=\"locale in form.locales\"\n           ng-show=\"locale === selectedLocale\"/>\n  </div>\n  <div ng-if=\"form.rows && form.rows > 1\"\n       ng-class=\"{\'form-group\' : !$last}\">\n    <textarea class=\"form-control\"\n              ng-disabled=\"form.readonly\"\n              sf-field-model=\"replaceAll\" ng-model=\"$$value$$[locale]\"\n              rows=\"{{form.rows ? form.rows : 5}}\"\n              ng-repeat=\"locale in form.locales\"\n              ng-show=\"locale === selectedLocale\"></textarea>\n  </div>\n  <span class=\"help-block\" sf-message=\"form.description\"></span>\n</div>\n");}]);
angular.module('sfLocalizedString', [
  'schemaForm',
  'sfLocalizedStringTemplates'
]).config(['schemaFormProvider', 'schemaFormDecoratorsProvider', 'sfBuilderProvider', 'sfPathProvider',
    function(schemaFormProvider,  schemaFormDecoratorsProvider, sfBuilderProvider, sfPathProvider) {

    var locStr = function(name, schema, options) {
      if (schema.type === 'object' && schema.format == 'localizedString') {
        var f = schemaFormProvider.stdFormObj(name, schema, options);
        f.key  = options.path;
        f.type = 'localizedstring';
        if(!f.languages) {
          f.languages = {en: 'English'};
        }
        f.locales = Object.keys(f.languages);
        f.validationMessage = {
          completed: 'All localized fields must be completed'
        };
        f.$validators = {
          completed: function(value) {
            if (value && Object.keys(value).length > 0) {
              return Object.keys(value).filter(function(key){
                return f.locales.indexOf(key) > -1 && value[key] && '' !== value[key];
              }).length === f.locales.length;
            }

            return true;
          }
        };
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

  }])
  .controller('LocalizedStringController', ['$scope', function($scope){
    $scope.$watch('ngModel.$modelValue', function() {
      if ($scope.ngModel.$validate) {
        // Make sure that allowInvalid is always true so that the model is preserved when validation fails
        $scope.ngModel.$options = $scope.ngModel.$options || {};
        $scope.ngModel.$options = {allowInvalid: true};
        $scope.ngModel.$validate();
        if ($scope.ngModel.$invalid) { // The field must be made dirty so the error message is displayed
          $scope.ngModel.$dirty = true;
          $scope.ngModel.$pristine = false;
        }
      }
      else {
        $scope.ngModel.$setViewValue(ngModel.$viewValue);
      }
    }, true);

    $scope.$watch('form', function() {
      $scope.form.disableErrorState = $scope.form.hasOwnProperty('readonly') && $scope.form.readonly;
      $scope.selectedLocale = $scope.form.locales && $scope.form.locales.length > 0 ? $scope.form.locales[0] : '';
    });

    $scope.selectLocale = function (locale) {
      $scope.selectedLocale = locale;
      $scope.open = false;
    };

    $scope.toggleDropdown = function() {
      $scope.open = !$scope.open;
    };

    $scope.$on('sfLocalizedStringLocaleChanged', function(event, locale){
      $scope.selectLocale(locale);
    });

    $scope.open = false;

  }]);
