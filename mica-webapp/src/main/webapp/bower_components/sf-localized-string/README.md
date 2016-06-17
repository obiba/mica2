Schema Form Localizes String Add-on
===================================
 
**sf-localized-string** add-on is used to upload files and store the file DTO's in the form model.

Installation
------------

```
$ bower install sf-localized-string --save
```

Alternatively:

```
$ bower install https://github.com/obiba/sf-localized-string.git#<release-number> --save
```


Make sure to include `sf-localized-string.min.js` in your index file and load the module in your application:

```
var myModule = angular.module('myModule', [
 ...
 'sfLocalizedString',
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
  "type":"localizedstring",
  "key":"name",
  "showLocales": true
}
```

