angular.module("sfCheckboxgroupTemplates", []).run(["$templateCache", function($templateCache) {$templateCache.put("src/templates/checkboxgroup-template.html","<div class=\"checkboxgroup schema-form-checkboxgroup {{form.HtmlClass}}\">\n	<div ng-controller=\"schemaFormCheckboxgroupController\">\n		<div class=\"has-error\" ng-show=\"showMessage\">\n			<span class=\"help-block\">{{message}}</span>\n		</div>\n	</div>\n</div>");}]);
angular.module('sfCheckboxgroup', ['schemaForm', 'sfCheckboxgroupTemplates'])
	.config(['schemaFormProvider', 'schemaFormDecoratorsProvider', 'sfBuilderProvider', 
		function (schemaFormProvider, schemaFormDecoratorsProvider, sfBuilderProvider) {
			schemaFormDecoratorsProvider.defineAddOn(
        'bootstrapDecorator',
        'checkboxgroup',
        'src/templates/checkboxgroup-template.html',
        sfBuilderProvider.stdBuilders
      );
		}])

	.controller('schemaFormCheckboxgroupController', ['$scope', function($scope) {
		var keys = $scope.form.items.map(function (val) { return val.key[0]; });
    var min = $scope.form.minChecked ? parseInt($scope.form.minChecked) : 0;
    $scope.showMessage = false;
    $scope.message = 'Minimum ' + min + '.';

    var models = [];

    if ($scope.model) {
      $scope.$on('schemaFormValidate', function () {
        models = keys.map(function (k) {
          return $scope.model[k];
        });

        var enough = models.filter(function (e) { return e; }).length >= min;
        if (enough) {
          $scope.$broadcast('schemaForm.error', 'minimumChecked', true);
          $scope.showMessage = false;
        } else {
          $scope.$broadcast('schemaForm.error', 'minimumChecked', false);
          $scope.showMessage = true;
        }
      });
    }
	}]);