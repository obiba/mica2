/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
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
      return $resource(contextPath + '/ws/config/:target/taxonomy', {}, {
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

        if (!attribute) {
          attribute = {key: key, value: null};
          attributes.push(attribute);
        }

        attribute.value = value;
      }

      function getAttribute(attributes, key, defaultValue) {
        var field = attributes.filter(function(a) {
            return a.key === key;
          }).pop();

        return field ? field.value : defaultValue;
      }

      function setAttributeAndRemoveIfNull(attributes, key, value) {
        if (null === value || 'null' === value) {
          // remove null value
          var index = attributes.map(function(attribute){
            return attribute.key;
          }).indexOf(key);

          if (-1 !== index) {
            attributes.splice(index, 1);
          }

        } else {
          setAttribute(attributes, key, value);
        }
      }

      function getIntegerAttribute(content, key, defaultValue) {
        var value = defaultValue ? defaultValue : null;
        if (content && content.attributes) {
          value = getAttribute(content.attributes, key, defaultValue);
        }

        return parseInt(value);
      }

      function getBoolAttribute(content, key) {
        if (content && content.attributes) {
          return 'true' === getAttribute(content.attributes, key, 'false');
        }

        return false;
      }

      this.getType = function(content) {
        if (content && content.attributes) {
          return getAttribute(content.attributes, 'type', 'keyword');
        }

        return 'keyword';
      };

      this.getTypeMap = function(content) {
        var type = self.getType(content);

        return {
          value: type,
          title: $filter('translate')('global.types.'+type)
        };

      };

      this.setType = function(attributes, type) {
        setAttribute(attributes, 'type', type);
      };

      this.getField = function(content) {
        if (content && content.attributes) {
          return getAttribute(content.attributes, 'field', content.name);
        }

        return '';
      };

      this.setField = function(attributes, field) {
        setAttribute(attributes, 'field', field);
      };

      this.generateAlias = function(field, isRangeAgg) {
        if (!field) {
          return '';
        }

        var postfix = isRangeAgg ? '-range' : '';
        return field.replace(/\./g, '-') + postfix;
      };

      this.setRange = function(attributes, range) {
        setAttribute(attributes, 'range', range+'');
      };

      this.getRange = function(content) {
        return getBoolAttribute(content, 'range');
      };

      this.setLocalized = function(attributes, localized) {
        setAttribute(attributes, 'localized', localized+'');
      };

      this.getLocalized = function(content) {
        return getBoolAttribute(content, 'localized');
      };

      this.setFacet = function(attributes, facet) {
        setAttribute(attributes, 'facet', facet+'');
      };

      this.getFacet = function(content) {
        return getBoolAttribute(content, 'facet');
      };

      this.setFacetExpanded = function(attributes, expanded) {
        setAttribute(attributes, 'facetExpanded', expanded+'');
      };

      this.getFacetExpanded = function(content) {
        return getBoolAttribute(content, 'facetExpanded');
      };

      this.setFacetPosition = function(attributes, position) {
        setAttributeAndRemoveIfNull(attributes, 'facetPosition', position+'');
      };

      this.getFacetPosition = function(content) {
        return getIntegerAttribute(content, 'facetPosition', 0);
      };

      this.getTermsSortKeyMap = function(content) {
        var termsSortKey = self.getTermsSortKey(content);

        return {
          value: termsSortKey,
          title: $filter('translate')('global.'+(termsSortKey ? termsSortKey : 'default'))
        };

      };

      this.getTermsSortKey = function(content) {
        if (content && content.attributes) {
          return getAttribute(content.attributes, 'termsSortKey', null);
        }

        return null;
      };

      this.setTermsSortKey = function(attributes, value) {
        setAttributeAndRemoveIfNull(attributes, 'termsSortKey', value);
      };

      this.setHidden = function(attributes, localized) {
        setAttribute(attributes, 'hidden', localized+'');
      };

      this.getHidden = function(content) {
        return getBoolAttribute(content, 'hidden');
      };

      this.isStatic = function(content, forClassName) {
        var isStatic = getBoolAttribute(content, 'static');
        if (!isStatic && content) {
          // If this vocabulary is common or belongs to another className, then it is static
          var forClassNameAttribute = getAttribute(content.attributes, 'forClassName', null);

          if (forClassNameAttribute === null) {
            return false;
          } else {
            return forClassNameAttribute !== forClassName;
          }
        }

        return isStatic;
      };

      this.setAlias = function(attributes, content) {
        setAttribute(attributes, 'alias', self.generateAlias(content.field, content.rangeAggregation));
      };

      this.getAliases = function(vocabularies, excludeVocabulary) {
        var aliasList = [];
        (vocabularies || []).filter(function (vocabulary) {
          return vocabulary !== excludeVocabulary;
        }).forEach(function(vocabulary) {
          var alias = getAttribute(vocabulary.attributes, 'alias', null);
          if (alias && vocabulary) {
            aliasList.push(alias);
          }
        });

        return aliasList;
      };

      this.setForClassName = function (attributes, className) {
        setAttribute(attributes, 'forClassName', className);
      };

      this.getForClassName = function (content) {
        if (content && content.attributes) {
          return getAttribute(content.attributes, 'forClassName', '');
        }

        return null;
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

      function getTypeMap() {
        return [
          {
            'value': 'string',
            'name': $filter('translate')('global.types.string')
          },
          {
            'value': 'keyword',
            'name': $filter('translate')('global.types.keyword')
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

      function getTermsSortKeyMap() {
        return [
          {
            'value': null,
            'name': $filter('translate')('global.default')
          },
          {
            'value': 'name',
            'name': $filter('translate')('global.name')
          },
          {
            'value': 'title',
            'name': $filter('translate')('global.title')
          }
        ];
      }

      function parseRangeValue(value) {
        return  value && '*' !== value ? parseInt(value) : undefined;
      }

      function serializeRangeValue(from, to) {
        return  (from || '*') + ':' + (to || '*');
      }

      function getSiblingNames(siblings) {
        if (siblings) {
          return siblings.map(function(s) {
            return s.name;
          });
        }

        return [];
      }

      function createNameFormField(helpTextKey, validationMessageKey, names, props) {
        var nameFormField = {
          key: 'name',
          description: '<p class="help-block">' + $filter('translate')(helpTextKey) + '</p>',
          validationMessage: {
            unique: $filter('translate')(validationMessageKey)
          },
          $validators: {
            unique: function (value) {
              if (value && names) {
                return names.indexOf(value) === -1;
              }
              return true;
            }
          }
        };

        return props ? angular.extend(nameFormField, props) : nameFormField;
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

      function getVocabularyFormData(forClassName, content, possibleClassNames, siblings, onRangeChange) {
        var isStatic = VocabularyAttributeService.isStatic(content, forClassName);

        var classNameTitleMap = [
          {
            'value': '',
            'name': $filter('translate')('global.default')
          }
        ];

        if (Array.isArray(possibleClassNames)) {
          possibleClassNames.forEach(choice => classNameTitleMap.push({ 'name': choice, 'value': choice }));
        }

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
              'hidden': {
                'title': $filter('translate')('taxonomy-config.criterion-dialog.hidden'),
                'type': 'boolean'
              },
              'localized': {
                'title': $filter('translate')('global.localized'),
                'type': 'boolean',
                'readonly': isStatic
              },
              'field': {
                'title': $filter('translate')('global.field'),
                'type': 'string',
                'format':'typeahead',
                'required': true,
                'readonly': isStatic
              },
              'type': {
                'title': $filter('translate')('type'),
                'type': 'string',
                'default': 'string',
                'readonly': isStatic
              },
              'termsSortKey': {
                'title': $filter('translate')('taxonomy-config.criterion-dialog.terms-sort-key'),
                'default': null,
                'type': [
                  'null',
                  'string'
                ],
                'readonly': isStatic,
              },
              'rangeAggregation': {
                'title': $filter('translate')('taxonomy-config.criterion-dialog.range-aggregation'),
                'type': 'boolean',
                'readonly': isStatic
              },
              'facet': {
                'title': $filter('translate')('taxonomy-config.criterion-dialog.facet'),
                'type': 'boolean'
              },
              'facetPosition': {
                'title': $filter('translate')('taxonomy-config.criterion-dialog.facet-position'),
                'type': 'integer',
                'default': 0
              },
              'facetExpanded': {
                'title': $filter('translate')('taxonomy-config.criterion-dialog.facet-expanded'),
                'type': 'boolean'
              },
              'forClassName': {
                'title': $filter('translate')('taxonomy-config.criterion-dialog.for-classname-attribute'),
                'default': '',
                'type': 'string',
                'readonly': isStatic
              }
            }
          },
          definition: [
            // name is added below
            {
              'type':'localizedstring',
              'key':'title',
              'showLocales': true
            },
            {
              'type':'localizedstring',
              'key':'description',
              'showLocales': true,
              'rows': 5
            },
            {
              'type': 'tabs',
              'htmlClass': 'voffset4',
              'tabs': [
                {
                  'title': $filter('translate')('global.definition'),
                  'items': [
                    {
                      'key': 'type',
                      'type': 'select',
                      'titleMap': getTypeMap(),
                      'description': '<p class="help-block">' + $filter('translate')('taxonomy-config.criterion-dialog.type-help') + '</p>'
                    },
                    {
                      'key': 'repeatable',
                      'description': '<p class="help-block">' + $filter('translate')('taxonomy-config.criterion-dialog.repeatable-help') + '</p>'
                    },
                    'localized',
                    {
                      'key': 'field',
                      'type': 'typeahead',
                      'description': '<p class="help-block">' + $filter('translate')('taxonomy-config.criterion-dialog.field-help') + '</p>'
                    },
                    {
                      'key': 'rangeAggregation',
                      'condition': 'model.type && model.type !== \'string\'',
                      'description': '<p class="help-block">' + $filter('translate')('taxonomy-config.criterion-dialog.range-aggregation-help') + '</p>',
                      'onChange': onRangeChange
                    },
                    {
                      'key': 'forClassName',
                      'type': 'select',
                      'titleMap': classNameTitleMap,
                      'description': '<p class="help-block">' + $filter('translate')('taxonomy-config.criterion-dialog.for-classname-attribute-help') + '</p>'
                    }
                  ]
                },
                {
                  'title': $filter('translate')('global.display'),
                  'items': [
                    {
                      'key': 'hidden',
                      'description': '<p class="help-block">' + $filter('translate')('taxonomy-config.criterion-dialog.hidden-help') + '</p>'
                    },
                    {
                      'key': 'termsSortKey',
                      'type': 'select',
                      'titleMap': getTermsSortKeyMap(),
                      'description': '<p class="help-block">' + $filter('translate')('taxonomy-config.criterion-dialog.term-sort-key-help') + '</p>'
                    },
                    {
                      'key': 'facet',
                      'description': '<p class="help-block">' + $filter('translate')('taxonomy-config.criterion-dialog.facet-help') + '</p>'
                    },
                    {
                      'type': 'section',
                      'htmlClass': 'well well-sm no-margin',
                      'condition': 'model.facet',
                      'items': [
                        {
                          'key': 'facetPosition',
                          'description': '<p class="help-block">' + $filter('translate')('taxonomy-config.criterion-dialog.facet-position-help') + '</p>',
                          'destroyStrategy': 'retain'
                        },
                        {
                          'key': 'facetExpanded',
                          'description': '<p class="help-block">' + $filter('translate')('taxonomy-config.criterion-dialog.facet-expanded-help') + '</p>',
                          'destroyStrategy': 'retain'
                        }
                      ]
                    }
                  ]
                }
              ]
            }
          ],
          model: {}
        };

        data.definition.unshift(
          createNameFormField(
            'taxonomy-config.criterion-dialog.name-help',
            'taxonomy-config.criterion-dialog.unique-name',
            getSiblingNames(siblings)
          )
        );

        convertToLocalizedString(data.model, content, ['title', 'description']);
        if (content) {
          data.model.type = VocabularyAttributeService.getType(content);
          data.model.name = content.name;
          data.model.field = VocabularyAttributeService.getField(content);
          data.model.repeatable = content.repeatable;
          data.model.hidden = VocabularyAttributeService.getHidden(content);
          data.model.localized = VocabularyAttributeService.getLocalized(content);
          data.model.facet = VocabularyAttributeService.getFacet(content);
          data.model.facetPosition = VocabularyAttributeService.getFacetPosition(content);
          data.model.facetExpanded = VocabularyAttributeService.getFacetExpanded(content);
          data.model.termsSortKey = VocabularyAttributeService.getTermsSortKey(content);
          data.model.rangeAggregation = VocabularyAttributeService.getRange(content);
          data.model.forClassName = VocabularyAttributeService.getForClassName(content);
        }
        return data;
      }

      function getTermFormData(forClassName, content, valueType, siblings, vocabulary) {
        var isStatic = VocabularyAttributeService.isStatic(vocabulary);
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
              'from': {
                'type': 'number',
                'title': $filter('translate')('global.from'),
                'readonly': isStatic
              },
              'to': {
                'type': 'number',
                'title': $filter('translate')('global.to'),
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
              'keywords': {
                'type': 'object',
                'format': 'localizedString',
                'title': $filter('translate')('global.keywords')
              }
            }
          },
          definition: [
            // name is added below
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
          model: {isRange: ['keyword','string'].indexOf(valueType) < 0}
        };

        data.definition.unshift(
          createNameFormField(
            'taxonomy-config.term-dialog.name-help',
            'taxonomy-config.term-dialog.unique-name',
            getSiblingNames(siblings),
            {condition: '!model.isRange'}
          )
        );

        if (valueType !== 'string') {
          if (content) {
            var parts = content.name.split(':');
            data.model.from = parseRangeValue(parts[0]);
            data.model.to = parseRangeValue(parts[1]);
          }
        }

        if (content) {
          convertToLocalizedString(data.model, content, ['title', 'description', 'keywords']);
          data.model.name = content.name;
        }
        return data;
      }

      this.validateModel = function(data, model) {
        if ('criterion' === model.type) {
          var alias = VocabularyAttributeService.generateAlias(data.model.field, data.model.rangeAggregation);
          return (model.aliases || []).indexOf(alias) === -1;
        }

        return true;
      };

      this.updateModel = function(data, model) {
        switch (model.type) {
          case 'taxonomy':
            convertFromLocalizedString(data.model, model.content, ['title', 'description']);
            break;
          case 'criterion':
            model.content = model.content || {};
            convertFromLocalizedString(data.model, model.content, ['title', 'description']);

            model.content.attributes = model.content.attributes || [];
            VocabularyAttributeService.setForClassName(model.content.attributes, data.model.forClassName);

            if (data.model.type) {
              VocabularyAttributeService.setType(model.content.attributes, data.model.type);
            }

            if (data.model.hasOwnProperty('hidden')) {
              VocabularyAttributeService.setHidden(model.content.attributes, data.model.hidden);
            }

            if (data.model.hasOwnProperty('localized')) {
              VocabularyAttributeService.setLocalized(model.content.attributes, data.model.localized);
            }

            if (data.model.hasOwnProperty('facet')) {
              VocabularyAttributeService.setFacet(model.content.attributes, data.model.facet);

              if (data.model.facet) {
                if (data.model.hasOwnProperty('facetPosition')) {
                  VocabularyAttributeService.setFacetPosition(model.content.attributes, data.model.facetPosition);
                }

                if (data.model.hasOwnProperty('facetExpanded')) {
                  VocabularyAttributeService.setFacetExpanded(model.content.attributes, data.model.facetExpanded);
                }
              } else {
                // remove face related properties
                model.content.attributes = model.content.attributes.filter(function(element) {
                  return ['facetPosition', 'facetExpanded'].indexOf(element.key) === -1;
                });
              }
            }

            if (data.model.hasOwnProperty('termsSortKey')) {
              VocabularyAttributeService.setTermsSortKey(model.content.attributes, data.model.termsSortKey);
            }

            VocabularyAttributeService.setField(model.content.attributes, data.model.field);
            VocabularyAttributeService.setRange(model.content.attributes, data.model.rangeAggregation);
            VocabularyAttributeService.setAlias(model.content.attributes, data.model);
            model.content.name = data.model.name;
            model.content.repeatable = data.model.repeatable;
            break;

          case 'term':
            model.content = model.content || {};
            convertFromLocalizedString(data.model, model.content, ['title', 'description', 'keywords']);
            if (model.valueType !== 'string' && data.model.isRange) {
              model.content.name = serializeRangeValue(data.model.from, data.model.to);
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
            return getVocabularyFormData(model.forClassName, model.content, model.possibleClassNames, model.siblings, model.onRangeChange);
          case 'term':
            return getTermFormData(model.forClassName, model.content, model.valueType || 'string', model.siblings, model.vocabulary);
        }

        throw new Error('EntityTaxonomySchemaFormService - invalid type:' + model.type);
      };

      return this;
    }])

  .service('EntitySchemaFormFieldsService', [
    function () {
      function isObject(node) {
        return Object.prototype.toString.call(node) === '[object Object]';
      }

      function isLeafObject(node) {
        return Object.keys(node).every(function (k) {
          return !isObject(node[k]);
        });
      }

      function traverse(node, parentKey, paths, fields) {
        if (isObject(node)) {
          if (isLeafObject(node)) {
            fields.push(paths.join('.'));
          }

          Object.keys(node).forEach(function (k) {
            var v = node[k];
            traverse(v, k, 'properties' === parentKey ? paths.concat(k) : paths, fields);
          });
        }
      }

      /**
       * Combines the input schemas and returns one hierarchical one
       * The order of schemas are significant in that the first contains the second and so on. Consider the schemas of
       * a study, they schemas must be order as:
       * {
       *  id: 'studies', schema: {...},
       *  id: 'populations', schema: {...},
       *  id: 'dataCollectionEvents', schema: {...}
       * }
       *
       * @param schemas
       * @returns {*}
       */
      this.concatenateSchemas = function(schemas) {
        if (!schemas) {
          return [];
        }

        var result = {};
        var parent = result;

        schemas.forEach(function (item) {
          parent[item.id] = {
            properties: {
              model: {
                properties: item.schema.properties
              }
            }
          };
          parent = parent[item.id].properties;
        });

        // do not require the id of the parent schema
        return result[Object.keys(result)[0]];
      };

      this.getFieldNames = function (schema) {
        var fields = [];
        // concatenateSchemas(schemas);
        traverse(schema, null, [], fields);
        return fields;
      };

    }]);
