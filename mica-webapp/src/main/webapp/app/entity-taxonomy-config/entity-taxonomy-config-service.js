/*
 * Copyright (c) 2016 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

mica.entityTaxonomyConfig

  .factory('EntityTaxonomyConfigResource', ['$resource',
    function ($resource) {
      return $resource('ws/config/:target/taxonomy', {}, {
        'get': {method: 'GET', errorHandler: true},
        'save': {method: 'PUT', errorHandler: true}
      });
    }])

  .service('VocabularyAttributeService', [
    '$filter',
    function($filter){
      var self = this;
      function setAttribute(attributes, key, value) {
        var attribute = attributes.filter(function(attribute){
          return attribute.key === key;
        }).pop();

        attribute = attribute || {key: key, value: null};
        attribute.value = value;
        attributes.push(attribute);
      }

      function getAttribute(attributes, key, defaultValue) {
        var field = attributes.filter(function(a) {
            return a.key === key;
          }).pop();

        return field ? field.value : defaultValue;
      }

      this.getType = function(content) {
        if (content && content.attributes) {
          return getAttribute(content.attributes, 'type', 'string');
        }

        return 'string';
      };

      this.getTypeMap = function(content) {
        var type = self.getType(content);

        return {
          value: type,
          title: $filter('translate')('types.'+type)
        };

      };

      this.setType = function(attributes, type) {
        setAttribute(attributes, 'type', type);
      };

      this.getField = function(content) {
        if (content && content.attributes) {
          return getAttribute(content.attributes, 'field', '');
        }

        return '';
      };

      this.setField = function(attributes, field) {
        setAttribute(attributes, 'field', field);
        var alias = field.replace(/\./, '-');
        setAttribute(attributes, 'alias', alias);
      };

      this.getLocalized = function(content) {
        if (content && content.attributes) {
          return getAttribute(content.attributes, 'localized', false);
        }

        return false;
      };

      this.setLocalized = function(attributes, localized) {
        setAttribute(attributes, 'localized', localized);
      };

      this.isStatic = function(content) {
        if (content && content.attributes) {
          return getAttribute(content.attributes, 'static', false);
        }

        return false;
      };

      return this;
    }])

  .service('EntityTaxonomyService', [
    function() {

      this.getTermsCount = function(content) {
        return content && content.terms ? content.terms.length : 0;
      };

    }])

  .service('EntityTaxonomySchemaFormService', [
    '$filter',
    'VocabularyAttributeService',
    function($filter, VocabularyAttributeService) {

      function convertToLocalizedString(model, content, fields) {

        function convert(field) {
          var o = {};
          field.forEach(function(field){
            o[field.locale] = field.text;
          });

          return o;
        }

        if (content) {
          fields.forEach(function (field) {
            if (content[field]) {
              model[field] = convert(content[field]);
            }
          });
        }
      }

      function convertFromLocalizedString(model, content, fields) {

        function convert(field) {
          var o = [];
          Object.keys(field).forEach(function(lang){
            o.push({locale: lang, text: field[lang]});
          });

          return o;
        }

        fields.forEach(function(field) {
          if(model[field]) {
            content[field] = convert(model[field]);
          }
        });

      }

      function getTaxonomyFormData(content) {

        var data = {
          schema: {
            'type': 'object',
            'properties': {
              'title': {
                'type': 'object',
                'format': 'localizedString',
                'title': $filter('translate')('title'),
                'minLength': 2,
                'required': true,
              },
              'description': {
                'type': 'object',
                'format': 'localizedString',
                'title': $filter('translate')('description'),
                'required': true
              }
            }
          },
          definition: [
            {
              'type':'localizedstring',
              'key':'title',
              'showLocales': true
            },
            {
              'type':'localizedstring',
              'key':'description',
              'showLocales': true,
              'rows': 10
            }
          ],
          model: {}
        };

        convertToLocalizedString(data.model, content, ['title', 'description']);
        return data;
      }

      function getTypeMap() {
        return [
            {
              'value': 'string',
              'name': $filter('translate')('global.types.string')
            },
            {
              'value': 'integer',
              'name': $filter('translate')('global.types.integer')
            },
            {
              'value': 'decimal',
              'name': $filter('translate')('global.types.decimal')
            }
          ];
      }

      function getVocabularyFormData(content) {
        var isStatic = VocabularyAttributeService.isStatic(content);
        var data = {
          schema: {
            'type': 'object',
            'properties': {
              'name': {
                'type': 'string',
                'title': $filter('translate')('name'),
                'required': true,
                'readonly': isStatic
              },
              'title': {
                'type': 'object',
                'format': 'localizedString',
                'title': $filter('translate')('global.title'),
                'minLength': 2,
                'required': true
              },
              'description': {
                'type': 'object',
                'format': 'localizedString',
                'title': $filter('translate')('description'),
                'required': true
              },
              'repeatable': {
                'title': $filter('translate')('global.repeatable'),
                'type': 'boolean',
                'readonly': isStatic
              },
              'localized': {
                'title': $filter('translate')('global.localized'),
                'type': 'boolean',
                'readonly': isStatic
              },
              'field': {
                'title': $filter('translate')('global.field'),
                'type': 'string',
                'required': true,
                'readonly': isStatic
              },
              'type': {
                'title': $filter('translate')('type'),
                'type': 'string',
                'readonly': isStatic
              }
            }
          },
          definition: [
            {
              'key': 'name',
              'description': '<p class="help-block">' + $filter('translate')('taxonomy-config.criterion-dialog.name-help') + '</p>'
            },
            {
              'type':'localizedstring',
              'key':'title',
              'showLocales': true
            },
            {
              'type':'localizedstring',
              'key':'description',
              'showLocales': true,
              'rows': 10
            },
            {
              'key': 'repeatable',
              'description': '<p class="help-block">' + $filter('translate')('taxonomy-config.criterion-dialog.repeatable-help') + '</p>'
            },
            'localized',
            {
              'key': 'field',
              'description': '<p class="help-block">' + $filter('translate')('taxonomy-config.criterion-dialog.field-help') + '</p>'
            },
            {
              'key': 'type',
              'type': 'select',
              'titleMap': getTypeMap(),
              'description': '<p class="help-block">' + $filter('translate')('taxonomy-config.criterion-dialog.field-help') + '</p>'
            }
          ],
          model: {}
        };

        convertToLocalizedString(data.model, content, ['title', 'description']);
        if (content) {
          data.model.type = VocabularyAttributeService.getType(content).value;
          data.model.name = content.name;
          data.model.field = VocabularyAttributeService.getField(content);
          data.model.repeatable = content.repeatable;
          data.model.localized = VocabularyAttributeService.getLocalized(content);
        }
        return data;
      }


      function getTermFormData(content, valueType) {
        var data = {
          schema: {
            'type': 'object',
            'properties': {
              'name': {
                'type': 'string',
                'title': $filter('translate')('name'),
                'required': true
              },
              'from': {
                'type': 'number',
                'title': $filter('translate')('global.from'),
                'required': true
              },
              'to': {
                'type': 'number',
                'title': $filter('translate')('global.to'),
                'required': true
              },
              'title': {
                'type': 'object',
                'format': 'localizedString',
                'title': $filter('translate')('global.title'),
                'minLength': 2,
                'required': true
              },
              'description': {
                'type': 'object',
                'format': 'localizedString',
                'title': $filter('translate')('description'),
                'required': true
              },
              'keywords': {
                'type': 'object',
                'format': 'localizedString',
                'title': $filter('translate')('global.keywords')
              }
            }
          },
          definition: [
            {
              'key': 'name',
              'condition': '!model.isRange',
              'description': $filter('translate')('taxonomy-config.term-dialog.name-help')
            },
            {
              'type': 'fieldset',
              'condition': 'model.isRange',
              'items': [
                {
                  'type': 'section',
                  'htmlClass': 'row',
                  'items': [
                    {
                      'type': 'section',
                      'htmlClass': 'col-xs-6',
                      'items': [
                        {
                          'key': 'from'
                        }
                      ]
                    },
                    {
                      'type': 'section',
                      'htmlClass': 'col-xs-6',
                      'items': [
                        {
                          'key': 'to'
                        }
                      ]
                    }
                  ]
                }
              ]
            },
            {
              'type':'localizedstring',
              'key':'title',
              'showLocales': true
            },
            {
              'type':'localizedstring',
              'key':'description',
              'showLocales': true,
              'rows': 10
            },
            {
              'type':'localizedstring',
              'key':'keywords',
              'showLocales': true,
              'rows': 10,
              'description': $filter('translate')('taxonomy-config.term-dialog.keywords-help')
            }
          ],
          model: {isRange: valueType !== 'string'}
        };

        if (valueType !== 'string') {
          if (content) {
            var parts = content.name.split(':');
            data.model.from = parseInt(parts[0]);
            data.model.to = parseInt(parts[1]);
          }
        }

        if (content) {
          convertToLocalizedString(data.model, content, ['title', 'description', 'keywords']);
          data.model.name = content.name;
        }
        return data;
      }

      this.updateModel = function(data, model) {
        switch (model.type) {
          case 'taxonomy':
            convertFromLocalizedString(data.model, model.content, ['title', 'description']);
            break;
          case 'criterion':
            model.content = model.content || {};
            convertFromLocalizedString(data.model, model.content, ['title', 'description']);

            model.content.attributes = model.content.attributes || [];

            if (data.model.type) {
              VocabularyAttributeService.setType(model.content.attributes, data.model.type);
            }

            if (data.model.localized) {
              VocabularyAttributeService.setLocalized(model.content.attributes, data.model.localized);
            }

            VocabularyAttributeService.setField(model.content.attributes, data.model.field);
            model.content.name = data.model.name;
            model.content.repeatable = data.model.repeatable;
            break;

          case 'term':
            model.content = model.content || {};
            convertFromLocalizedString(data.model, model.content, ['title', 'description', 'keywords']);
            if (model.valueType !== 'string' && data.model.isRange) {
              model.content.name = data.model.from + ':' + data.model.to;
            } else {
              model.content.name = data.model.name;
            }

            break;

          default:
            throw new Error('EntityTaxonomySchemaFormService - invalid type:' + model.type);
        }

      };

      this.getFormData = function(model) {
        switch (model.type) {
          case 'taxonomy':
            return getTaxonomyFormData(model.content);
          case 'criterion':
            return getVocabularyFormData(model.content);
          case 'term':
            return getTermFormData(model.content, model.valueType || 'string');
        }

        throw new Error('EntityTaxonomySchemaFormService - invalid type:' + model.type);
      };

      return this;
    }]);


