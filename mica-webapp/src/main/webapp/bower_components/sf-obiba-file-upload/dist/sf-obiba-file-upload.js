angular.module("templates", []).run(["$templateCache", function($templateCache) {$templateCache.put("src/templates/sf-obiba-file-upload.html","<div ng-controller=\"sfObibaFileUploadController\" sf-field-model>\n  <div class=\"form-group\">\n    <label translate>description</label>\n    <input sf-field-model=\"replaceAll\"\n           ng-disabled=\"form.readonly\"\n           type=\"text\"\n           class=\"form-control\"\n           ng-model=\"$$value$$.description\"\n           placeholder=\"{{form.description}}\">\n\n    <attachment-input ng-if=\"!form.readonly\"\n                        sf-field-model=\"replaceAll\"\n                        files=\"$$value$$.obibaFiles\"\n                        multiple=\"form.schema.multiple\"></attachment-input>\n\n    <attachment-list ng-if=\"form.readonly\"\n                     href-builder=\"getDownloadUrl\"\n                     files=\"$$value$$.obibaFiles\"\n                     sf-field-model=\"replaceAll\"></attachment-list>\n  </div>\n</div>");}]);
angular.module('sfObibaFileUpload', [
  'schemaForm',
  'templates',
  'ngObibaMica'
]).config(['schemaFormProvider', 'schemaFormDecoratorsProvider', 'sfPathProvider', 'sfBuilderProvider',
  function (schemaFormProvider, schemaFormDecoratorsProvider, sfPathProvider, sfBuilderProvider) {

    var obibaFileUpload = function (name, schema, options) {

      if (schema.type === 'object' && (schema.format === 'obibaFile' || schema.format === 'obibaFiles')) {
        var f = schemaFormProvider.stdFormObj(name, schema, options);
        f.key = options.path;
        f.type = 'obibaFileUpload';
        options.lookup[sfPathProvider.stringify(options.path)] = f;
        return f;
      }
    };

    schemaFormProvider.defaults.object.unshift(obibaFileUpload);

    schemaFormDecoratorsProvider.defineAddOn(
      'bootstrapDecorator',           // Name of the decorator you want to add to.
      'obibaFileUpload',                      // Form type that should render this add-on
      'src/templates/sf-obiba-file-upload.html',  // Template name in $templateCache
      sfBuilderProvider.stdBuilders   // List of builder functions to apply.
    );

  }])
  .controller('sfObibaFileUploadController', ['$scope', 'ngObibaMicaUrl', 
    function ($scope, ngObibaMicaUrl) {
      console.log('>>>> Model', $scope.model);
      $scope.getDownloadUrl = function(attachment) {
        return ngObibaMicaUrl.getUrl('SchemaFormAttachmentDownloadResource')
          .replace(':path', attachment.path)
          .replace(':attachmentName', attachment.fileName)
          .replace(':attachmentId', attachment.id)
          .replace('//', '/');
      };

      $scope.$watch('form', function() {
        console.log('####', $scope.form);
      }, true);
    }]);
