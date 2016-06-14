angular.module('sfObibaFileUpload', [
  'schemaForm',
  'sfObibaFileUploadTemplates',
  'ngObibaMica'
]).config(['schemaFormProvider', 'schemaFormDecoratorsProvider', 'sfPathProvider', 'sfBuilderProvider',
  function (schemaFormProvider, schemaFormDecoratorsProvider, sfPathProvider, sfBuilderProvider) {

    var obibaFileUpload = function (name, schema, options) {

      if (schema.type === 'object' && schema.format === 'obibaFiles') {
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
      $scope.getDownloadUrl = function(attachment) {
        return ngObibaMicaUrl.getUrl('SchemaFormAttachmentDownloadResource')
          .replace(':path', attachment.path)
          .replace(':attachmentName', attachment.fileName)
          .replace(':attachmentId', attachment.id)
          .replace('//', '/');
      };
    }]);
