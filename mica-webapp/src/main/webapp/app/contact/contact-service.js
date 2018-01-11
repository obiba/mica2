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

mica.contact
  .factory('ContactsSearchResource', ['$resource', 'ContactSerializationService',
    function ($resource, ContactSerializationService) {
      return $resource('ws/draft/persons/_search?', {}, {
        'search': {method: 'GET', params: {query: '@query', 'exclude': '@exclude'}, transformResponse: ContactSerializationService.deserializeList}
      });
    }])
  .factory('ContactSerializationService', ['LocalizedValues',
    function (LocalizedValues) {

      var it = this;

      this.serialize = function(person) {

        if (person.institution) {
          person.institution.name = LocalizedValues.objectToArray(person.institution.name);
          person.institution.department = LocalizedValues.objectToArray(person.institution.department);
          if (person.institution.address) {
            person.institution.address.street = LocalizedValues.objectToArray(person.institution.address.street);
            person.institution.address.city = LocalizedValues.objectToArray(person.institution.address.city);
            if (person.institution.address.country) {
              person.institution.address.country = {'iso': person.institution.address.country};
            }
          }
        }

        return person;
      };

      this.deserializeList = function (personsList) {

        personsList = angular.fromJson(personsList);

        if (personsList.persons) {
          personsList.persons = personsList.persons.map(function (person) {
            return it.deserialize(person);
          });
        }

        return personsList;
      };

      this.deserialize = function(person) {

        person = angular.copy(person);

        if (person.institution) {
          person.institution.name = LocalizedValues.arrayToObject(person.institution.name);
          person.institution.department = LocalizedValues.arrayToObject(person.institution.department);
          if (person.institution.address) {
            person.institution.address.street = LocalizedValues.arrayToObject(person.institution.address.street);
            person.institution.address.city = LocalizedValues.arrayToObject(person.institution.address.city);
            if (person.institution.address.country) {
              person.institution.address.country = person.institution.address.country.iso;
            }
          }
        }

        return person;
      };

      return this;
    }]);
