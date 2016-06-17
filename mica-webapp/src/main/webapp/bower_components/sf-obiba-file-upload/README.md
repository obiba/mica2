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

If you wish to override the validation messages, use the `sfObibaFileUploadOptions` provider. Through the provider you can either set the message or a message key that can be found in your translation files:
 
```
var myModule = angular.module('myModule', [
 ...
 'sfObibaFileUpload',
]).config([..., 'sfObibaFileUploadOptionsProvider', function(...,sfObibaFileUploadOptionsProvider){
  sfObibaFileUploadOptionsProvider.setValidationMessageKey('missingFiles', 'missing-files');
  sfObibaFileUploadOptionsProvider.setValidationMessageKey('minItems', 'min-items');
  
  // --or--
  
  sfObibaFileUploadOptionsProvider.setValidationMessageKey('missingFiles', 'Missing files.');
  sfObibaFileUploadOptionsProvider.setValidationMessageKey('minItems', 'Must have 3 files.');
  
}]);
```

If you specify a minItems value greater than `1`, multiple is automatically `true` so several files can be uploaded.

Todo
----

- Implement `maxItems`.
- When listing image files, provide thumbnail.
- open PDF and document files in the browser rather downloading them.  
