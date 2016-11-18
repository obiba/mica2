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

mica.contact
  .factory('ContactsSearchResource', ['$resource',
    function ($resource) {
      return $resource('ws/draft/persons/_search?', {}, {
        'search': {method: 'GET', params: {query: '@query', 'exclude': '@exclude'}}
      });
    }])
  .factory('ContactSerializationService', ['LocalizedValues',
    function (LocalizedValues) {
      this.serialize = function(person) {
        person.institution.name = LocalizedValues.objectToArray(person.institution.name);
        person.institution.department = LocalizedValues.objectToArray(person.institution.department);
        person.institution.address.street = LocalizedValues.objectToArray(person.institution.address.street);
        person.institution.address.city = LocalizedValues.objectToArray(person.institution.address.city);

        return person;
      };

      this.deserialize = function(person) {
        var copy = angular.copy(person);
        copy.institution.name = LocalizedValues.arrayToObject(person.institution.name);
        copy.institution.department = LocalizedValues.arrayToObject(person.institution.department);
        if (person.institution.address) {
          copy.institution.address.street = LocalizedValues.arrayToObject(person.institution.address.street);
          copy.institution.address.city = LocalizedValues.arrayToObject(person.institution.address.city);
        }
        return copy;
      };

      return this;
    }]);
