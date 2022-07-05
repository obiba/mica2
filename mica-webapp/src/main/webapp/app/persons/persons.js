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

mica.persons = angular.module('mica.persons', [
  'obiba.form',
  'mica.config',
  'obiba.notification',
  'obiba.mica.localized',
  'pascalprecht.translate',
  'ui.bootstrap'
]);

mica.persons
  .config(['$routeProvider', function ($routeProvider) {
    $routeProvider
      .when('/persons', {
        templateUrl: 'app/admin/views/persons.html',
        reloadOnSearch: false,
        access: {
          authorizedRoles: ['mica-administrator']
        }
      })
      .when('/person/:id', {
        templateUrl: 'app/admin/views/person.html',
        reloadOnSearch: false,
        access: {
          authorizedRoles: ['mica-administrator']
        }
      })
      .when('/person/:id/edit', {
        templateUrl: 'app/admin/views/person.html',
        reloadOnSearch: false,
        access: {
          authorizedRoles: ['mica-administrator']
        }
      })
      .when('/person/:id/revisions', {
        templateUrl: 'app/admin/views/person.html',
        reloadOnSearch: false,
        access: {
          authorizedRoles: ['mica-administrator']
        }
      });
  }])
  .service('EntityTitleService', ['$filter', function($filter) {
    function translate(entityType, plural) {
      return plural ?
        $filter('translate')(entityType === 'network' ? 'networks' : 'studies') :
        $filter('translate')(`${entityType}.label`);
    }

    this.translate = translate;

    return this;
  }])
  .service('EntityMembershipService', ['$filter', '$translate', 'LocalizedValues',
    function($filter, $translate, LocalizedValues) {

      function groupRolesByEntity(type, memberships) {
        const lang = $translate.use();
        const data = {
          title: $filter('translate')(type),
          entities: []
        };

        let entityMap = {};

        memberships.forEach(membership => {
          let entity = entityMap[membership.parentId];
          if (!entity) {
            entity = entityMap[membership.parentId] = {};
            entity.id = membership.parentId;
            entity.acronym = LocalizedValues.forLang(membership.parentAcronym, lang);
            entity.name = LocalizedValues.forLang(membership.parentName, lang);
            entity.roles = [$filter('translate')(`contact.label.${membership.role}`)];
            if ('obiba.mica.PersonDto.StudyMembershipDto.meta' in membership) {
              entity.url = `/${membership['obiba.mica.PersonDto.StudyMembershipDto.meta'].type}/${entity.id}`;
            } else {
              entity.url = `/network/${entity.id}`;
            }
          } else {
            entity.roles = [].concat([$filter('translate')(`contact.label.${membership.role}`)], entity.roles).sort();
          }
        });

        data.entities = Object.values(entityMap) || [];

        return data;
      }

      this.groupRolesByEntity = groupRolesByEntity;
      return this;
  }]);
