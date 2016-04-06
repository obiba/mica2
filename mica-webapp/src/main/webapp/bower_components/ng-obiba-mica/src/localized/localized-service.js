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
      this.for = function (values, lang, keyLang, keyValue) {
        if (angular.isArray(values)) {
          var result = values.filter(function (item) {
            return item[keyLang] === lang;
          });

          if (result && result.length > 0) {
            return result[0][keyValue];
          }
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
    });
