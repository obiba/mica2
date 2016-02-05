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

/**
 * Visitor used to walk the JSON query and return a node matching input name
 * @param name
 * @constructor
 */
var FindNodeVisitor = function (name) {
  this.node = null;
  this.name = name;
};

/**
 * Returns TRUE if node is found
 * @param node
 * @returns {boolean}
 */
FindNodeVisitor.prototype.accept = function (node) {

  if (this.name === node.name) {
    this.node = node;
    return true;
  }
  // must go on!
  return false;
};

/**
 * Returns the found node while traversing JSON
 * @returns {null|*}
 */
FindNodeVisitor.prototype.getNode = function () {
  return this.node;
};


/**
 * Criteria Item builder
 */
function CriteriaItemBuilder(LocalizedValues, useLang) {
  var criteria = {
    id: '',
    taxonomy: [],
    vocabulary: [],
    term: null,
    selectedTerms: [],
    target: '',
    lang: useLang,
    itemTitle: '',
    itemDescription: '',
    itemParentTitle: '',
    itemParentDescription: ''
  };

  this.target = function(value) {
    criteria.target = value;
    return this;
  };

  this.taxonomy = function(value) {
    criteria.taxonomy = value;
    return this;
  };

  this.vocabulary = function(value) {
    criteria.vocabulary = value;
    return this;
  };

  this.term = function(value) {
    criteria.term = value;
    return this;
  };

  this.selectedTerm = function(value) {
    criteria.selectedTerms.push(value);
    return this;
  };

  this.selectedTerms = function(values) {
    criteria.selectedTerms = values;
    return this;
  };

  this.build = function() {
    criteria.id = criteria.taxonomy.name + '::' + criteria.vocabulary.name;

    if (criteria.term) {
      criteria.id += ':' + criteria.term.name;

      criteria.itemTitle = LocalizedValues.forLocale(criteria.term.title, criteria.lang);
      criteria.itemDescription = LocalizedValues.forLocale(criteria.term.description,criteria.lang);
      criteria.itemParentTitle = LocalizedValues.forLocale(criteria.vocabulary.title, criteria.lang);
      criteria.itemParentDescription = LocalizedValues.forLocale(criteria.vocabulary.description, criteria.lang);
      if (!criteria.itemTitle) {
        criteria.itemTitle = criteria.term.name;
      }
      if (!criteria.itemParentTitle) {
        criteria.itemParentTitle = criteria.vocabulary.name;
      }
    } else {
      criteria.itemTitle = LocalizedValues.forLocale(criteria.vocabulary.title, criteria.lang);
      criteria.itemDescription = LocalizedValues.forLocale(criteria.vocabulary.description, criteria.lang);
      criteria.itemParentTitle = LocalizedValues.forLocale(criteria.taxonomy.title, criteria.lang);
      criteria.itemParentDescription = LocalizedValues.forLocale(criteria.taxonomy.description, criteria.lang);
      if (!criteria.itemTitle) {
        criteria.itemTitle = criteria.vocabulary.name;
      }
      if (!criteria.itemParentTitle) {
        criteria.itemParentTitle = criteria.taxonomy.name;
      }
    }

    return criteria;
  };

}

/**
 * Abstract class for all criteria builders
 * @param root
 * @param taxonomies
 * @param LocalizedValues
 * @param lang
 * @constructor
 */
function AbstractCriteriaBuilder(root, taxonomies, LocalizedValues, lang) {
  this.target = null;
  this.root = root;
  this.taxonomies = taxonomies;
  this.criteriaList = [];
  this.LocalizedValues = LocalizedValues;
  this.lang = lang;

  /**
   * Called by the leaf visitor to create a criteria
   * @param targetTaxonomy
   * @param targetVocabulary
   * @param targetTerms
   */
  this.addCriteria = function(targetTaxonomy, targetVocabulary, targetTerms) {
    var self = this;

    var foundTaxonmy = this.taxonomies.filter(function(taxonomy){
      return targetTaxonomy === taxonomy.name;
    });

    if (foundTaxonmy.length === 0 ) {
      throw new Error('Could not find taxonomy:', targetTaxonomy);
    }

    foundTaxonmy = foundTaxonmy[0];

    var foundVocabulary = foundTaxonmy.vocabularies.filter(function(vocabulary){
      return targetVocabulary === vocabulary.name;
    });

    if (foundVocabulary.length === 0 ) {
      throw new Error('Could not find vocabulary:', targetVocabulary);
    }

    var foundCount = 0;
    foundVocabulary = foundVocabulary[0];

    var builder = new CriteriaItemBuilder(self.LocalizedValues, self.lang)
      .target(self.target)
      .taxonomy(foundTaxonmy)
      .vocabulary(foundVocabulary);

    if (foundVocabulary.terms) {
      foundVocabulary.terms.some(function (term) {
        if (targetTerms.indexOf(term.name) !== -1) {
          builder.selectedTerm(term).build();
          foundCount++;

          // stop searching
          return foundCount === targetTerms.length;
        }
      });
    }

    self.criteriaList.push(builder.build());
  };
}

/**
 * This method is where a criteria gets created
 */
AbstractCriteriaBuilder.prototype.visitLeaf = function (/*node*/) {

};

/**
 * Returns all the criterias found
 * @returns {Array}
 */
AbstractCriteriaBuilder.prototype.getCriteriaList = function (/*node*/) {
  return this.criteriaList;
};

/**
 * Node condition visitor
 * @param node
 */
AbstractCriteriaBuilder.prototype.visitCondition =function (node) {
  this.visit(node.args[0]);
  this.visit(node.args[1]);
};

/**
 * General purpose node visitor
 * @param node
 */
AbstractCriteriaBuilder.prototype.visit = function(node) {
  console.log('Visit', node);

  switch (node.name) {
    case 'and':
    case 'or':
      this.visitCondition(node);
      break;
    case 'in':
    case 'out':
    case 'eq':
    case 'le':
    case 'lt':
    case 'ge':
    case 'gt':
    case 'between':
      this.visitLeaf(node);
      break;
    case 'not':
    case 'match':
      break;
    default:
  }
};

/**
 * Builds a criteria list for this target
 */
AbstractCriteriaBuilder.prototype.build = function() {
  var self = this;
  this.root.args.forEach(function(node){
    self.visit(node);
  });
};

/**
 * Variable criteria builder
 * @param root
 * @param taxonomies
 * @param criteriaItemBuilder
 * @param lang
 * @constructor
 */

var VariableCriteriaBuilder = function(root, taxonomies, LocalizedValues, lang) {
  AbstractCriteriaBuilder.call(this, root, taxonomies, LocalizedValues, lang);
  this.target = 'variable';

  this.parserFieldName = function(name) {
    var re = /(\w+)__(\w+)/;
    var m = re.exec(name.replace(/attributes\./, '').replace(/\.\w+$/,''));

    if (m && m.length > 2) {
      return {taxonomy: m[1], vocabulary: m[2]};
    }

    throw new Error('Invalid field name', name);
  };
};

VariableCriteriaBuilder.prototype = Object.create(AbstractCriteriaBuilder.prototype);

VariableCriteriaBuilder.prototype.visitLeaf = function(node) {
  console.log('VariableCriteriaBuilder');
  var field = node.args[0];
  var values = node.args[1];
  var searchInfo = this.parserFieldName(field);
  console.log(node.name, 'F:', field, 'V:', values);

  this.addCriteria(searchInfo.taxonomy, searchInfo.vocabulary, values instanceof Array ? values : [values]);
};

/**
 * TODO Given a target node, it adds a RQLQuery
 * @param parser
 * @constructor
 */

var RqlQueryBuilder = function(parser) {
  this.rqlParser = parser;
};

/**
 * TODO implement real code...
 * @param name
 * @param field
 * @param args
 * @returns {*}
 */
RqlQueryBuilder.prototype.node = function(name, field, args) {
  var q = ':name(:f,(:args))'.replace(/:name/,name).replace(/:f/, field).replace(/:args/, args.join(','));
  return this.rqlParser.parse(q).args[0];
};
/**
 * Module services and factories
 */
angular.module('obiba.mica.search')
  .factory('TaxonomiesSearchResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('TaxonomiesSearchResource'), {}, {
        'get': {
          method: 'GET',
          isArray: true,
          errorHandler: true
        }
      });
    }])

  .factory('TaxonomiesResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('TaxonomiesResource'), {}, {
        'get': {
          method: 'GET',
          isArray: true,
          errorHandler: true
        }
      });
    }])

  .factory('TaxonomyResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('TaxonomyResource'), {}, {
        'get': {
          method: 'GET',
          errorHandler: true
        }
      });
    }])

  .factory('JoinQuerySearchResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('JoinQuerySearchResource'), {}, {
        'variables': {
          method: 'GET',
          errorHandler: true,
          params: {type: 'variables'}
        },
        'studies': {
          method: 'GET',
          errorHandler: true,
          params: {type: 'studies'}
        },
        'networks': {
          method: 'GET',
          errorHandler: true,
          params: {type: 'networks'}
        },
        'datasets': {
          method: 'GET',
          errorHandler: true,
          params: {type: 'datasets'}
        }
      });
    }])

  .factory('JoinQueryCoverageResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('JoinQueryCoverageResource'), {}, {
        'get': {
          method: 'GET',
          errorHandler: true
        }
      });
    }])

  .factory('VocabularyResource', ['$resource', 'ngObibaMicaUrl',
    function ($resource, ngObibaMicaUrl) {
      return $resource(ngObibaMicaUrl.getUrl('VocabularyResource'), {}, {
        'get': {
          method: 'GET',
          errorHandler: true
        }
      });
    }])

  .service('ObibaSearchConfig', function () {
    var options = {
      networks: {
        showSearchTab:1
      },
      studies: {
        showSearchTab:1
      },
      datasets: {
        showSearchTab:1
      },
      variables: {
        showSearchTab:1
      }
    };

    this.setOptions = function (newOptions) {
      if (typeof(newOptions) === 'object') {
        Object.keys(newOptions).forEach(function (option) {
          if (option in options) {
            options[option] = newOptions[option];
          }
        });
      }
    };

    this.getOptions = function () {
      return angular.copy(options);
    };

  })

  /**
   * Service providing RQL related functionality
   */
  .service('RqlQueryService', [
    '$q',
    'TaxonomiesResource',
    'LocalizedValues',
    function ($q, TaxonomiesResource, LocalizedValues) {
      // TODO add the rest of the target taxonomy caches
      var variableTaxonomies = null;

      /**
       * Walks through the JSON query and calls a visitor
       * @param node
       * @param visitor
       * @returns {boolean}
       */
      function walk(node, visitor) {
        console.log('findNode', node);
        if (node instanceof RqlQuery) {

          if (visitor.accept(node)) {
            return true;
          } else if (node.args) {
            node.args.some(function (child) {
              return walk(child, visitor);
            });
          }
        }
        return false;
      }

      /**
       * Finds a query node by name
       * @param root
       * @param name
       * @returns {null|*}
       */
      function findNode(root, name) {
        var visitor = new FindNodeVisitor(name);
        walk(root, visitor);
        return visitor.getNode();
      }

      /**
       * Creates a criteria item
       * @param target
       * @param taxonomy
       * @param vocabulary
       * @param term
       * @param lang
       * @returns A criteria item
       */
      this.createCriteria = function(target, taxonomy, vocabulary, term, lang) {

        return new CriteriaItemBuilder(LocalizedValues, lang)
          .target(target)
          .taxonomy(taxonomy)
          .vocabulary(vocabulary)
          .term(term)
          .build();
      };

      /**
       * Finds the variable query node
       * @param root
       * @returns {null|*}
       */
      this.variableNode = function (root) {
        return findNode(root, 'variable');
      };

      /**
       * Builds the variable query criterias
       * @param root
       * @param lang
       * @returns {*}
       */
      this.buildVariableCriteria = function(root, lang) {
        var self = this;
        var deferred = $q.defer();

        function build() {
          var variableNode = self.variableNode(root);
          if (variableNode) {
            var builder = new VariableCriteriaBuilder(variableNode, self.variableTaxonomies, LocalizedValues, lang);

            builder.build();
            deferred.resolve(builder.getCriteriaList());
          }
        }

        if (variableTaxonomies) {
          build();
        } else {
          TaxonomiesResource.get({
            target: 'variable'
          }).$promise.then(function(response) {
            self.variableTaxonomies = response;
            build();
          });
        }

        return deferred.promise;
      };

      this.normalizeAggregationFieldName = function(type, taxonomy, vocabulary) {

        // TODO complete and create type-based functions
        switch (type) {
          case 'variable':
            return 'attributes.' + taxonomy + '__' + vocabulary + '.und';
        }

        throw new Error('Not implemented: ' + taxonomy + ' ' + vocabulary);
      };

      /**
       * Append the aggregate and facet for criteria term listing.
       *
       * @param type
       * @param query
       * @param taxonomy
       * @param vocabulary
       * @returns the new query
       */
      this.prepareCriteriaTermsQuery = function(type, query, taxonomy, vocabulary) {
        var parsedQuery = new RqlParser().parse(query);
        var aggregate = new RqlQuery('aggregate');
        var facet = new RqlQuery('facet');
        aggregate.args.push(this.normalizeAggregationFieldName(type, taxonomy, vocabulary));
        parsedQuery.args.some(function(arg) {
          if(arg.name === type) {
            arg.args.push(aggregate);
            return true;
          }
          return false;
        });
        parsedQuery.args.push(facet);

        return parsedQuery.serializeArgs(parsedQuery.args);
      };

      /**
       * Append the aggregate and bucket operations to the variable.
       *
       * @param query
       * @param bucketArgs
       * @returns the new query
       */
      this.prepareCoverageQuery = function(query, bucketArgs) {
        var parsedQuery = new RqlParser().parse(query);
        var aggregate = new RqlQuery('aggregate');
        var bucket = new RqlQuery('bucket');
        bucketArgs.forEach(function(b){
          bucket.args.push(b);
        });
        aggregate.args.push(bucket);
        parsedQuery.args.forEach(function(arg) {
          if(arg.name === 'variable') {
            arg.args.push(aggregate);
          }
        });
        return parsedQuery.serializeArgs(parsedQuery.args);
      };

    }]);
