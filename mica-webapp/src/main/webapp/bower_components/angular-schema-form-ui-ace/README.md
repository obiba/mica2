ui-ace add-on
=================

This ui-ace add-on uses as the name implies the ui-ace plugin to provide a ace editor for schema form. [ace](https://github.com/ajaxorg/ace) as well as [ui-ace](https://github.com/angular-ui/ui-ace) is used.

ace is highly customizable and this add-on takes an options object via `aceOptions` in the form. More info below at [Options](#Options).

Installation
------------
The editor is an add-on to the Bootstrap decorator. To use it, just include
`bootstrap-ui-ace.min.js` *after* `dist/bootstrap-decorator.min.js`.

Easiest way is to install is with bower, this will also include dependencies:
```bash
$ bower install angular-schema-form-ui-ace
```

You'll need to load a few additional files to use the editor:

**Be sure to load this projects files after you load angular schema form**

1. Angular
2. The [ace](https://github.com/ajaxorg/ace) source file
3. The [ui-ace](https://github.com/angular-ui/ui-ace) source file
4. **Angular Schema Form**
5. The Angular Schema Form Tinymce files (this project)
6. Translation files for whatever language you want to use (optional) [Documentation](https://github.com/ajaxorg/ace)

Example

```HTML
<script type="text/javascript" src="/bower_components/angular/angular.min.js"></script>
<script type="text/javascript" src="/bower_components/angular-sanitize/angular-sanitize.min.js"></script>
<script type="text/javascript" src="/bower_components/ace-builds/src-min-noconflict/ace.js"></script>
<script type="text/javascript" src="/bower_components/angular-ui-ace/ui-ace.js"></script>

<script type="text/javascript" src="/bower_components/angular-schema-form/schema-form.min.js"></script>
<script type="text/javascript" src="/bower_components/angular-schema-form-ui-ace/bootstrap-ui-ace.js"></script>

```

Usage
-----
The tinymce add-on adds a new form type, `wysiwyg`, and a new default
mapping.

|  Form Type     |   Becomes    |
|:---------------|:------------:|
|  ace           |  a ace widget |


| Schema             |   Default Form type  |
|:-------------------|:------------:|
| "type": "string"   |   ace   |


Options
-------
The `ace` form takes one option, `aceOptions`. This is an object with any
and all options availible to ace. A full list of these can be found [here](https://github.com/ajaxorg/ace).

### Example
This example replaces the standard toolbar with one we choose.

```javascript
{
  "key": "content",
  "aceOptions": {
  	useWrapMode : true,
  	showGutter: false,
  	theme:'twilight',
  	mode: 'xml',
  	firstLineNumber: 5
  	onLoad: aceLoaded,
  	onChange: aceChanged
  }
},
```
