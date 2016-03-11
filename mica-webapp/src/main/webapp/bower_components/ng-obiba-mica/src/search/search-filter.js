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

angular.module('obiba.mica.search')

  .filter('regex', function() {
    return function(elements, regex, fields) {
      var out = [];

      try {
        var pattern = new RegExp(regex, 'i');
        out = elements.filter(function(element) {
          return fields.some(function(field){
            return pattern.test(element[field]);
          });
        });

      } catch(e) {
      }

      return out;
    };
  })

  .filter('orderBySelection', function() {
    return function (elements, selections) {
      if (!elements){
        return [];
      }

      var selected = [];
      var unselected = [];

      elements.forEach(function(element) {
        if (selections[element.key]) {
          selected.push(element);
        } else {
          unselected.push(element);
        }
      });

      return selected.concat(unselected);
    };
  })

  .filter('dceDescription', function() {
    return function(input) {
      return input.split(':<p>').map(function(d){
        return '<p>' + d;
      })[2];
    };
  });