/*
 * Copyright (c) 2015 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

angular.module('obiba.mica.localized')

  .service('LocalizedValues',
    function () {
      var self = this;
      this.for = function (values, lang, keyLang, keyValue) {
        if (angular.isArray(values)) {
          var result = values.filter(function (item) {
            return item[keyLang] === lang;
          });

          if (result && result.length > 0) {
            return result[0][keyValue];
          }
        } else if (angular.isObject(values)) {
          return self.for(Object.keys(values).map(function(k) {
            return {lang: k, value: values[k]};
          }), lang, keyLang, keyValue);
        }
        
        return '';
      };

      this.forLocale = function (values, lang) {
        var rval = this.for(values, lang, 'locale', 'text');
        if (rval === '') {
          rval = this.for(values, 'und', 'locale', 'text');
        }
        return rval;
      };

      this.forLang = function (values, lang) {
        var rval = this.for(values, lang, 'lang', 'value');
        if (rval === '') {
          rval = this.for(values, 'und', 'lang', 'value');
        }
        return rval;
      };

      this.getLocal = function () {
        return 'en';
      };

      this.formatNumber = function (val) {
        return (typeof val === 'undefined' && val === null && typeof val !== 'number') ? val : val.toLocaleString(this.getLocal());
      };

      this.arrayToObject = function (values) {
        var rval = {};
        if (values) {
          values.forEach(function(entry) {
            rval[entry.lang] = entry.value;
          });
        }
        return rval;
      };

      this.objectToArray = function (languages, values) {
        var rval = [];
        if (values && languages) {
          languages.forEach(function (lang) {
            rval.push({
              lang: lang,
              value: values[lang]
            });
          });
        }
        return rval;
      };
    })

  .service('LocalizedSchemaFormService', ['$filter', function ($filter) {

    this.translate = function(value) {
      if (!value) {
        return value;
      }
      if (typeof value === 'string') {
        return this.translateString(value);
      } else if (typeof value === 'object') {
        if (Array.isArray(value)) {
          return this.translateArray(value);
        } else {
          return this.translateObject(value);
        }
      }
      return value;
    };

    this.translateObject = function(object) {
      if (!object) {
        return object;
      }
      for (var prop in object) {
        if (object.hasOwnProperty(prop)) {
          if (typeof object[prop] === 'string') {
            object[prop] = this.translateString(object[prop]);
          } else if (typeof object[prop] === 'object') {
            if (Array.isArray(object[prop])) {
              object[prop] = this.translateArray(object[prop]);
            } else {
              object[prop] = this.translateObject(object[prop]);
            }
          } // else ignore
        }
      }
      return object;
    };

    this.translateArray = function(array) {
      if (!array) {
        return array;
      }
      var that = this;
      array.map(function (item) {
         return that.translate(item);
      });
      return array;
    };

    this.translateString = function(string) {
      if (!string) {
        return string;
      }
      return string.replace(/t\(([^\)]+)\)/g, function (match, p1) {
        return $filter('translate')(p1);
      });
    };

  }]);
