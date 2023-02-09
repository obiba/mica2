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
  .component('tagByConcept', {
    bindings: {
      taxos: '<', // taxonomies allowed through mica config
      current: '<', // current tags for study/initiative
      onUpdate: '&'
    },
    templateUrl: 'app/study/views/common/tag-by-concept-component.html',
    controllerAs: '$ctrl',
    controller: ['TaxonomyFilterResource', function (TaxonomyFilterResource) {
      var self = this;

      self.tags = [];

      function reset() {
        self.chosenTaxo = null;
        self.vocabChoices = [];
        self.termChoices = [];
        self.readyToTag = false;
      }

      function processNewtagsWithCurrent(attributes) {
        var res = attributes.filter(a => a);

        // should not add if new tag has the same namespace and name but no value
        // do not repeat tags with same namespace, name and value
        self.tags.forEach(tag => {
          var found = res.filter(r => r.namespace === tag.namespace && r.name === tag.name);
          if (found.values && tag.values && found.values.und !== tag.values.und) {
            res.push(tag);
          }

          if (!found || (Array.isArray(found) && found.length === 0)) {
            res.push(tag);
          }
        });

        return res;
      }

      reset();

      TaxonomyFilterResource.query().$promise.then(function (res) {
        if (res && res.length > 0) {
          self.taxoChoices = res.filter(r => self.taxos.indexOf(r.name) > -1);
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
        if (self.chosenVocabs && self.chosenVocabs.length === 1) {
          self.readyToTag = true;
          self.termChoices = (self.vocabChoices.filter(v => v.name === self.chosenVocabs[0])[0] || {terms: []}).terms;
        } else if (self.chosenVocabs && self.chosenVocabs.length > 0) {
          self.readyToTag = true;
          self.termChoices = [];
        } else {
          self.readyToTag = false;
          self.termChoices = [];
        }
      };

      self.addTags = function() {
        var attributes = [];

        var namespace = Array.isArray(self.chosenTaxo) ? self.chosenTaxo[0] : self.chosenTaxo;

        // more than one vocab forgoes the value
        if (Array.isArray(self.chosenVocabs) && self.chosenVocabs.length > 1) {
          self.chosenVocabs.forEach(vocab => {
            attributes.push({namespace: namespace, name: vocab});
          });
        } else if (Array.isArray(self.chosenVocabs) && self.chosenVocabs.length === 1) {
          var name = Array.isArray(self.chosenVocabs) ? self.chosenVocabs[0] : self.chosenVocabs;

          if (Array.isArray(self.chosenTerms) && self.chosenTerms.length > 0) {
            self.chosenTerms.forEach(term => {
              attributes.push({namespace: namespace, name: name, values: {und: term}});
            });
          } else {
            attributes.push({namespace: namespace, name: name});
          }
        }

        var processed = processNewtagsWithCurrent(attributes);
        self.current = processed;
        self.onUpdate({newTags: processed});
        reset();
      };
    }]
  });
