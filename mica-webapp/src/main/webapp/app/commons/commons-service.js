/*
 * Copyright (c) 2020 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

(function () {

  const ENTITY_STATE_FILTER = {
    ALL: 'ALL', PUBLISHED: 'PUBLISHED', UNDER_REVIEW: 'UNDER_REVIEW', IN_EDITION: 'IN_EDITION', TO_DELETE: 'TO_DELETE'
  };

  mica.commons.ENTITY_STATE_FILTER = ENTITY_STATE_FILTER;

  const PERSON_DUPLICATE_STATUS = {
    ERROR: 0,
    WARNING: 1,
    OK: 2
  };

  mica.commons.PERSON_DUPLICATE_STATUS = PERSON_DUPLICATE_STATUS;

  class PersonsDuplicateFinder {
    constructor($q, ContactsSearchResource) {
      this.$q = $q;
      this.ContactsSearchResource = ContactsSearchResource;
    }

    __buildQuery(searchFields) {
      if (!searchFields) {
        return '';
      }

      return Object.keys(searchFields)
        .reduce((acc, key) => {
          acc.push(`${key}:${searchFields[key].replaceAll(' ','\\ ')}`);
          return acc;
        }, [])
        .join(' AND ');
    }

    __processResponse(persons, searchFields, targetEmail) {
      const response = {status: PERSON_DUPLICATE_STATUS.OK, message:''};
      let name, email;

      if (persons) {
        if (searchFields.lastName) {
          response.status = PERSON_DUPLICATE_STATUS.WARNING;
          name = `${searchFields.firstName || ''} ${searchFields.lastName || ''}`.trim();
        }

        if (targetEmail && persons.filter(person => person.email === targetEmail).pop()) {
          response.status = PERSON_DUPLICATE_STATUS.ERROR;
          email = `${targetEmail}`;
        }

        if (name && email) {
          response.messageKey = 'contact.duplicate.name-email';
          response.messageArgs = [name, email];
        } else {
          response.messageKey = 'contact.duplicate.name';
          response.messageArgs = [name];
        }
      }

      return response;
    }

    searchPerson(person) {
      // cleanup empty fields
      const defaultSearchFields = {firstName: person.firstName, lastName: person.lastName};
      const searchFields = Object.keys(defaultSearchFields)
        .reduce((acc, key) => {
          if (defaultSearchFields[key] && defaultSearchFields[key] !== '') {
            acc[key] = defaultSearchFields[key];
          }
          return acc;
        }, {});


      let deferred = this.$q.defer();
      const query = this.__buildQuery(searchFields);
      if (query.length < 1) {
        deferred.resolve('');
      }

      this.ContactsSearchResource.search({
        query,
        from: 0,
        limit: mica.commons.DEFAULT_LIMIT,
        exclude: person.id
      }).$promise
        .then(result => {
          return deferred.resolve(this.__processResponse(result.persons, searchFields, person.email, person.id));
        })
        .catch((error) => deferred.reject(error));

      return deferred.promise;
    }
  }

  class QuerySearchUtils {
    /**
     * Utility function used to cleanup full-text queries in entity and person listing pages.
     *
     * @param text
     * @returns {*}
     */
    static cleanupQuery(text) {
      const ngObibaStringUtils = new obiba.utils.NgObibaStringUtils();
      const cleaners = [
        ngObibaStringUtils.cleanOrEscapeSpecialLuceneBrackets,
        ngObibaStringUtils.cleanDoubleQuotesLeftUnclosed,
        (text) => text.replace(/[!^~\\/]/g,'?'),
        (text) => null === text.match(/^".*"$/) && null === text.match(/\*$/) ? `${text}*` : text,
      ];

      let cleaned = text;
      if (null === text.match(/^".*"$/)) {
        // breakup texts separated by space
        let cleanedParts = [];
        text.split(/ /).forEach(part => {
          cleaned = part;
          cleaners.forEach(cleaner => cleaned = (cleaner.apply(null, [cleaned.trim()])));
          cleanedParts.push(cleaned);
        });

        cleaned = cleanedParts.join(' ');
      } else {
        cleaners.forEach(cleaner => cleaned = cleaner.apply(null, [cleaned.trim()]));
      }

      return cleaned && cleaned.length > 0 ? cleaned : null;
    }

    /**
     * Adds field to a query already cleaned by 'mica.commons.cleanupQuery'.
     *
     * @param query
     * @param queryField
     * @param locale
     * @returns {string|*}
     */
    static addQueryFields(query, queryField, locale) {
      const fieldName = true === queryField.localized ? `${queryField.field}.${locale}` : queryField.field;
      const analyzed = queryField.localized ? '.analyzed' : '';

      if ('all' === fieldName) {
        return query;
      } else if (null !== query.match(/^".*"$/)) {
        // whole word search
        return `${fieldName}${analyzed}:${query}`;
      }

      // Add fields to each query part
      return query.split(/ /).map(part => `${fieldName}${analyzed}:${part}`).join(' ');
    }
  }

  mica.commons.cleanupQuery = function(text) {
    return QuerySearchUtils.cleanupQuery(text);
  };

  mica.commons.addQueryFields = function(query, queryField, locale) {
    return QuerySearchUtils.addQueryFields(query, queryField, locale);
  };

  mica.commons.PersonsDuplicateFinder = PersonsDuplicateFinder;

  mica.commons

    .factory('CommentsResource', ['$resource',
      function ($resource) {
        return $resource(contextPath + '/ws/draft/:type/:id/comments', {}, {
          'save': {
            method: 'POST',
            params: {type: '@type', id: '@id'},
            headers : {'Content-Type' : 'text/plain' },
            errorHandler: true
          },
          'get': {method: 'GET', params: {type: '@type', id: '@id'}, errorHandler: true}
        });
      }])

    .factory('CommentResource', ['$resource',
      function ($resource) {
        return $resource('/ws/draft/:type/:id/comment/:commentId', {}, {
          'delete': {
            method: 'DELETE',
            params: {type: '@type', id: '@id', commentId: '@commentId'},
            errorHandler: true
          },
          'update': {
            method: 'PUT',
            params: {type: '@type', id: '@id', commentId: '@commentId'},
            headers : {'Content-Type' : 'text/plain' },
            errorHandler: true
          }
        });
      }])

    .factory('DocumentPermissionsService',
      function () {
        var factory = {};

        factory.state = function(value) {
          this.permissions = value.permissions;
          this.status = value.revisionStatus;
          return this;
        };

        factory.canView = function() {
          return this.permissions ? this.permissions.view : false;
        };

        factory.canEdit = function() {
          return this.permissions ? this.permissions.edit && this.status === 'DRAFT': false;
        };

        factory.canDelete = function() {
          return this.permissions ? this.permissions.delete && this.status === 'DELETED' : false;
        };

        factory.canPublish = function() {
          return this.permissions ? this.permissions.publish && this.status === 'UNDER_REVIEW': false;
        };

        return factory;
      })

    .factory('EntityStateFilterService', ['$location', function($location) {

      return {
        getFilterAndValidateUrl: () => {
          const search = $location.search();
          if ('filter' in search) {
            if (!(search.filter in ENTITY_STATE_FILTER)) {
              search.filter = ENTITY_STATE_FILTER.ALL;
              // validate URL
              $location.search(search).replace();
            }

            return search.filter;
          }

          return null;
        },

        updateUrl: (filter) => {
          let search = $location.search();
          search.filter = filter;
          $location.search(search);
        }
      };

    }]);

})();
