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

mica.study
  .directive('numberOfParticipants', [function () {
    return {
      restrict: 'E',
      templateUrl: 'app/study/views/common/number-of-participants.html',
      scope: {
        numberOfParticipants: '=',
        lang: '='
      },
      link: function (scope) {
        scope.$watch('numberOfParticipants.sample.noLimit', function(value) {
          if (value) {
            delete scope.numberOfParticipants.sample.number;
          }
        }, true);

        scope.$watch('numberOfParticipants.participant.noLimit', function(value) {
          if (value) {
            delete scope.numberOfParticipants.participant.number;
          }
        }, true);
      }
    };
  }])
  .component('annotationSelector', {
    bindings: {
      taxos: '<', // taxonomies allowed through mica config
      current: '<', // current tags for study/initiative
      onUpdate: '&'
    },
    templateUrl: 'app/study/views/common/annotation-selector-component.html',
    controllerAs: '$ctrl',
    controller: ['TaxonomyFilterResource', function (TaxonomyFilterResource) {
      var self = this;

      self.tags = [];

      function reset() {
        self.chosenTaxo = null;
        self.vocabChoices = [];
        self.readyToTag = false;
      }

      function processNewtagsWithCurrent(attributes) {
        var res = attributes.filter(a => a);

        // should not add if new tag has the same namespace and name but no value
        // do not repeat tags with same namespace, name and value
        self.tags.forEach(tag => {
          var found = res.filter(r => r.namespace === tag.namespace && r.name === tag.name);

          if (!found || (Array.isArray(found) && found.length === 0)) {
            res.push(tag);
          }
        });

        return res;
      }

      reset();

      TaxonomyFilterResource.query().$promise.then(function (res) {
        const taxos = self.taxos || [];
        if (res && res.length > 0) {
          self.taxoChoices = taxos.length > 0 ? res.filter(r => taxos.indexOf(r.name) > -1) : res;
        }

        return res;
      });

      self.$onChanges = function(changeObj) {
        if (changeObj.current && changeObj.current.currentValue) {
        // process tags
        // dto attribute has array of values
        if (Array.isArray(self.current)) {
          self.current.forEach(i => self.tags.push({namespace: i.namespace, name: i.name, values: Array.isArray(i.values) ? {und: (i.values.filter(v => v.lang === 'und')[0] || {value: null}).value} : i.values}));
        }
        }
      };

      self.taxoSelected = function() {
        if (self.chosenTaxo) {
          self.vocabChoices = (self.taxoChoices.filter(t => t.name === self.chosenTaxo)[0] || {vocabularies: []}).vocabularies;
        } else {
          self.vocabChoices = [];
          self.readyToTag = false;
        }
      };

      self.vocabsSelected = function() {
        self.readyToTag = self.chosenVocabs && self.chosenVocabs.length > 0;
      };

      self.addTags = function() {
        var attributes = [];

        var namespace = Array.isArray(self.chosenTaxo) ? self.chosenTaxo[0] : self.chosenTaxo;

        // more than one vocab forgoes the value
        if (Array.isArray(self.chosenVocabs) && self.chosenVocabs.length > 0) {
          self.chosenVocabs.forEach(vocab => {
            attributes.push({namespace: namespace, name: vocab});
          });
        }

        var processed = processNewtagsWithCurrent(attributes);
        self.current = processed;
        self.onUpdate({newTags: processed});
        reset();
      };
    }]
  })
  .component('annotationList', {
    bindings: {
      taxonomyNames: '<',
      attributes: '<',
      onUpdate: '&'
    },
    templateUrl: 'app/study/views/common/annotation-list-component.html',
    controllerAs: '$ctrl',
    controller: ['TaxonomyFilterResource', class AnnotationList {
      constructor(TaxonomyFilterResource) {
        this.TaxonomyFilterResource = TaxonomyFilterResource;
        this.attributesMap = {};
      }

      $onChanges() {
        if (this.attributes && this.taxonomyNames) {
          this.TaxonomyFilterResource.query().$promise
            .then(response => {
              this.taxonomies = (response || [])
                .filter(r => this.taxonomyNames.indexOf(r.name) > -1)
                .reduce((acc, tax) => {
                  acc[tax.name] = tax;
                  return acc;
                }, {});

              this.attributesMap = this.attributes.reduce((acc, att) => {
                const taxonomy = this.taxonomies[att.namespace];
                let namespaceMap = acc[att.namespace];
                if (!namespaceMap) {
                  namespaceMap = acc[att.namespace] = {title: taxonomy.title, description: taxonomy.description, names: {}};
                }
                const vocabulary = taxonomy.vocabularies.filter(vocabulary => vocabulary.name === att.name).pop();
                let nameMap = namespaceMap.names[att.name];
                if (!nameMap) {
                  namespaceMap.names[att.name] = {title: vocabulary.title, description: vocabulary.description, values: []};
                }

                return acc;
              }, {});
            });
        }
      }

      removeAttribute(taxName, vocName, termName) {
        let keep = [];

        if (termName) {
          keep = this.attributes.filter(attribute => attribute.namespace !== taxName || attribute.name !== vocName || attribute.values[0].value !== termName);
        } else if (vocName) {
          keep = this.attributes.filter(attribute => attribute.namespace !== taxName || attribute.name !== vocName);
        } else {
          keep = this.attributes.filter(attribute => attribute.namespace !== taxName);
        }

        keep = keep.map(attribute => {
          const clone = JSON.parse(JSON.stringify(attribute));

          clone.values = (attribute.values || []).reduce((acc, value) => {
            acc[value.lang] = value.value;
            return acc;
            }, {}
          );

          return clone;
        });
        this.onUpdate({newTags: keep});
      }
    }]
  });
