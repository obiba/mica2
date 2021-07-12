/*
 * Copyright (c) 2021 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

(function () {

  class Selector {
    constructor($translate, ContactsSearchResource, LocalizedValues) {
      this.$translate = $translate;
      this.ContactsSearchResource = ContactsSearchResource;
      this.LocalizedValues = LocalizedValues;
      this.autoRefreshed = false;
      this.result = {persons: [], total: 0, current: 0, count: 0, institutionsMap: {}, institutions: []};
      this._selected = '';
    }

    __clear() {
      this.result.persons = [];
      this.result.count = 0;
      this.result.total = 0;
      this.result.current = 0;
      this.autoRefreshed = false;
    }

    get selected() {
      return this._selected;
    }

    set selected(value) {
      console.debug('>> Selected', value);
      this._selected = value;
      this.onSelected({value: value});
    }

    onHighlighted(index, isLast, search) {
      if (isLast && !this.autoRefreshed) {
        this.autoRefreshed = true;
        this.find(search);
      }
    }
  }

  class PersonSelector extends Selector {
    constructor($translate, ContactsSearchResource, LocalizedValues) {
      super($translate, ContactsSearchResource, LocalizedValues);
    }

    __clear() {
      super.__clear();
    }

    find(search) {
      if (search) {
        if (!this.autoRefreshed) {
          this.__clear();
        }

        this.ContactsSearchResource.search(
          {
            query: search + '*',
            exclude: this.excludes,
            from: this.result.persons.length
          },
          function onSuccess(result) {
            if (result.persons) {
              this.result.persons = this.result.persons.concat(result.persons.map((person) => {
                if (person.institution && person.institution.name) {
                  const localized = this.LocalizedValues.forLang(person.institution.name, this.$translate.use());
                  person.institutionName = localized.length > 0 ? localized : '';
                }

                return person;
              }));
              this.result.total = result.total;
              this.result.current = this.result.persons.length;
              this.autoRefreshed = false;
            }
          }.bind(this));
      }
      else {
        this.__clear();
      }
    }
  }

  class InstitutionSelector extends Selector {
    constructor($translate, ContactsSearchResource, LocalizedValues) {
      super($translate, ContactsSearchResource, LocalizedValues);
    }

    __clear() {
      super.__clear();
      this.result.institutionsMap = {};
      this.result.institutions = [];

    }

    find(search) {
      if (search) {
        if (!this.autoRefreshed) {
          this.__clear();
        }
        const fieldName = 'institution.name.'+this.$translate.use();

        this.ContactsSearchResource.search(
          {
            query: fieldName+'.analyzed:'+search + '*',
            from: this.result.count,
            sort: fieldName
          },
          function onSuccess(result) {
            console.debug('ON INST SEARCH');
            if (result.persons) {
              this.result.count += result.persons.length;

              const institutionsMap = result.persons
                .filter(person => person.institution && 'name' in person.institution)
                .reduce((acc, person) => {
                  const institutionName = this.LocalizedValues.forLang(person.institution.name, this.$translate.use());
                  let institution = Object.assign({}, person.institution, { institutionName: institutionName });
                  let departmentName = '';
                  if (Object.keys(person.institution.department||{}).length > 0) {
                    departmentName = this.LocalizedValues.forLang(person.institution.department, this.$translate.use());
                    if (departmentName && departmentName.length > 0) {
                      institution.departmentName = departmentName;
                    }
                  }
                  acc[institutionName+departmentName] = institution;

                  return acc;
                }, {});

              this.result.institutionsMap = Object.assign({}, this.result.institutionsMap, institutionsMap);
              this.result.institutions = Object.values(this.result.institutionsMap);
              this.result.total = result.total;
              this.result.current = this.result.institutions.length;
              this.autoRefreshed = false;
            }
          }.bind(this));
      }
      else {
        this.__clear();
      }
    }
  }

  mica.commons
    .component('personSelector', {
      bindings: {
        onSelected: '&'
      },
      templateUrl: 'app/commons/components/person-institution-selector/person-component.html',
      controllerAs: '$ctrl',
      controller: [
        '$translate',
        'ContactsSearchResource',
        'LocalizedValues',
        PersonSelector
      ]
    });

  mica.commons
    .component('institutionSelector', {
      bindings: {
        onSelected: '&'
      },
      templateUrl: 'app/commons/components/person-institution-selector/institution-component.html',
      controllerAs: '$ctrl',
      controller: [
        '$translate',
        'ContactsSearchResource',
        'LocalizedValues',
        InstitutionSelector
      ]
    });

})();
