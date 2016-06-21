OBiBa's Schema Form File Upload Add-on
=================
 
**sf-obiba-file-upload** add-on is used to upload files and store the file DTO's in the form model.

Installation
------------

```
$ bower install sf-obiba-file-upload --save
```

Alternatively:

```
$ bower install https://github.com/obiba/sf-obiba-file-upload.git#<release-number> --save
```


Make sure to include `sf-obiba-file-upload.min.js` in your index file and load the module in your application:

```
var myModule = angular.module('myModule', [
 ...
 'sfObibaFileUpload',
]);
```

Usage
-----

The schema:

```
"myFiles": {
  "type": "object",
  "format": "obibaFiles",
  "title": "Title",      
  "multiple": true,
  "minItems": 3
}
```

The Definition:

```
{
  "key": "myFiles",
  "type": "obibaFileUpload",
  "emptyMessage": "No files uploaded",
  "validationMessage": {
    "missingFiles": "Missing files",
    "minItems": "Must upload at least 3 files".
  },
  "helpvalue": "Some help text"
}
```

To set default message values in case they are not defined in the form definition, use the `sfObibaFileUploadOptions` provider. Through the provider you can either set the message or a message key that can be found in your translation files:
 
```
var myModule = angular.module('myModule', [
 ...
 'sfObibaFileUpload',
]).config([..., 'sfObibaFileUploadOptionsProvider', function(...,sfObibaFileUploadOptionsProvider){
  sfObibaFileUploadOptionsProvider.setValidationMessage('missingFiles', 'missing-files');
  sfObibaFileUploadOptionsProvider.setValidationMessage('minItems', 'min-items');
  sfObibaFileUploadOptionsProvider.setGeneralMessage('emptyMessage', 'min-items'); // when the list of files is empty
  
  // --or--
  
  sfObibaFileUploadOptionsProvider.setValidationMessage('missingFiles', 'Missing files.');
  sfObibaFileUploadOptionsProvider.setValidationMessage('minItems', 'Must have 3 files.');
  sfObibaFileUploadOptionsProvider.setGeneralMessage('emptyMessage', 'No documents.'); // when the list of files is empty
  
}]);
```

If you specify a minItems value greater than `1`, multiple is automatically `true` so several files can be uploaded.

Todo
----

- Implement `maxItems`.
- When listing image files, provide a thumbnail viewer.
- Open PDF and document files in the browser rather downloading them.  
