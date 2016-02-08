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

/* exported QUERY_TYPES */
var QUERY_TYPES = {
  NETWORKS: 'networks',
  STUDIES: 'studies',
  DATASETS: 'datasets',
  VARIABLES: 'variables'
};

/* exported QUERY_TARGETS */
var QUERY_TARGETS = {
  NETWORK: 'network',
  STUDY: 'study',
  DATASET: 'dataset',
  VARIABLE: 'variable'
};

/* exported RQL_NODE */
var RQL_NODE = {
  // target nodes
  VARIABLE: 'variable',
  DATASET: 'dataset',
  STUDY: 'study',
  NETWORK: 'network',

  /* target children */
  LIMIT: 'limit',
  SORT: 'sort',
  AND: 'and',
  OR: 'or',
  NOT: 'not',
  FACET: 'facet',
  LOCALE: 'locale',
  AGGREGATE: 'aggregate',
  BUCKET: 'bucket',

  /* leaf criteria nodes */
  IN: 'in',
  OUT: 'out',
  EQ: 'eq',
  GT: 'gt',
  GE: 'ge',
  LT: 'lt',
  LE: 'le',
  BETWEEN: 'between',
  MATCH: 'match',
  EXISTS: 'exists',
  MISSING: 'missing'
};

/**
 * Criteria Item builder
 */
function CriteriaItemBuilder(LocalizedValues, useLang) {
  var criteria = {
    type: null,
    rqlQuery: null,
    lang: useLang || 'en',
    parent: null,
    children: []
  };

  this.type = function (value) {
    if (!RQL_NODE[value.toUpperCase()]) {
      throw new Error('Invalid node type:', value);
    }
    criteria.type = value;
    return this;
  };

  this.target = function (value) {
    criteria.target = value;
    return this;
  };

  this.parent = function (value) {
    criteria.parent = value;
    return this;
  };

  this.taxonomy = function (value) {
    criteria.taxonomy = value;
    return this;
  };

  this.vocabulary = function (value) {
    criteria.vocabulary = value;
    return this;
  };

  this.term = function (value) {
    criteria.term = value;
    return this;
  };

  this.rqlQuery = function (value) {
    criteria.rqlQuery = value;
    return this;
  };

  this.selectedTerm = function (value) {
    if (!criteria.selectedTerms) {
      criteria.selectedTerms = [];
    }

    criteria.selectedTerms.push(value);
    return this;
  };

  this.selectedTerms = function (values) {
    criteria.selectedTerms = values;
    return this;
  };

  /**
   * This is
   */
  function prepareForLeaf() {
    criteria.id = criteria.taxonomy.name + '::' + criteria.vocabulary.name;

    if (criteria.term) {
      criteria.id += ':' + criteria.term.name;

      criteria.itemTitle = LocalizedValues.forLocale(criteria.term.title, criteria.lang);
      criteria.itemDescription = LocalizedValues.forLocale(criteria.term.description, criteria.lang);
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
  }

  this.build = function () {
    if (criteria.taxonomy && criteria.vocabulary) {
      prepareForLeaf();
    }
    return criteria;
  };

}

/**
 * Abstract class for all criteria builders
 * @param rootRql
 * @param rootItem
 * @param taxonomies
 * @param LocalizedValues
 * @param lang
 * @constructor
 */
function AbstractCriteriaBuilder(rootRql, rootItem, taxonomies, LocalizedValues, lang) {

  /**
   * Helper to get a builder
   * @returns {CriteriaItemBuilder}
   */
  this.newCriteriaItemBuilder = function () {
    return new CriteriaItemBuilder(LocalizedValues, lang);
  };

  this.initialize = function (target) {
    this.target = target;
    this.rootRql = rootRql;
    this.taxonomies = taxonomies;
    this.LocalizedValues = LocalizedValues;
    this.lang = lang;
    this.rootItem = this.newCriteriaItemBuilder()
      .parent(rootItem)
      .type(this.target)
      .rqlQuery(this.rootRql)
      .target(this.target)
      .build();
  };

  /**
   * Called by the leaf visitor to create a criteria
   * @param targetTaxonomy
   * @param targetVocabulary
   * @param targetTerms
   * @param node
   */
  this.buildLeafItem = function (targetTaxonomy, targetVocabulary, targetTerms, node, parentItem) {
    var self = this;

    var foundTaxonmy = this.taxonomies.filter(function (taxonomy) {
      return targetTaxonomy === taxonomy.name;
    });

    if (foundTaxonmy.length === 0) {
      throw new Error('Could not find taxonomy:', targetTaxonomy);
    }

    foundTaxonmy = foundTaxonmy[0];

    var foundVocabulary = foundTaxonmy.vocabularies.filter(function (vocabulary) {
      return targetVocabulary === vocabulary.name;
    });

    if (foundVocabulary.length === 0) {
      throw new Error('Could not find vocabulary:', targetVocabulary);
    }

    var foundCount = 0;
    foundVocabulary = foundVocabulary[0];

    var builder = new CriteriaItemBuilder(self.LocalizedValues, self.lang)
      .type(node.name)
      .target(self.target)
      .taxonomy(foundTaxonmy)
      .vocabulary(foundVocabulary)
      .rqlQuery(node)
      .parent(parentItem);

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

    return builder.build();
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
AbstractCriteriaBuilder.prototype.getRootItem = function (/*node*/) {
  return this.rootItem;
};

/**
 * Node condition visitor
 * @param node
 * @param parentItem
 */
AbstractCriteriaBuilder.prototype.visitCondition = function (node, parentItem) {
  var item = this.newCriteriaItemBuilder().parent(parentItem).rqlQuery(node).type(node.name).build();
  parentItem.children.push(item);

  this.visit(node.args[0], item);
  this.visit(node.args[1], item);
};

/**
 * Node not visitor
 * @param node
 * @param parentItem
 */
AbstractCriteriaBuilder.prototype.visitNot = function (node, parentItem) {
  var item = this.newCriteriaItemBuilder().parent(parentItem).rqlQuery(node).type(node.name).build();
  parentItem.children.push(item);

  this.visit(node.args[0], item);
};

/**
 * General purpose node visitor
 * @param node
 * @param parentItem
 */
AbstractCriteriaBuilder.prototype.visit = function (node, parentItem) {

  // TODO needs to add more types
  switch (node.name) {
    case RQL_NODE.NOT:
      this.visitNot(node, parentItem);
      break;
    case RQL_NODE.AND:
    case RQL_NODE.OR:
      this.visitCondition(node, parentItem);
      break;

    case RQL_NODE.IN:
    case RQL_NODE.OUT:
    case RQL_NODE.EQ:
    case RQL_NODE.LE:
    case RQL_NODE.LT:
    case RQL_NODE.GE:
    case RQL_NODE.GT:
    case RQL_NODE.BETWEEN:
    case RQL_NODE.EXISTS:
    case RQL_NODE.MISSING:
      this.visitLeaf(node, parentItem);
      break;
    case RQL_NODE.MATCH:
      break;
    default:
  }
};

/**
 * Builds a criteria list for this target
 */
AbstractCriteriaBuilder.prototype.build = function () {
  var self = this;
  this.rootRql.args.forEach(function (node) {
    self.visit(node, self.rootItem);
  });
};

/**
 * Variable criteria builder
 * @param rootRql
 * @param taxonomies
 * @param criteriaItemBuilder
 * @param lang
 * @constructor
 */

var VariableCriteriaBuilder = function (rootRql, rootItem, taxonomies, LocalizedValues, lang) {
  AbstractCriteriaBuilder.call(this, rootRql, rootItem, taxonomies, LocalizedValues, lang);
  this.initialize(RQL_NODE.VARIABLE);

  this.parserFieldName = function (name) {
    var re = /(\w+)__(\w+)/;
    var m = re.exec(name.replace(/attributes\./, '').replace(/\.\w+$/, ''));

    if (m && m.length > 2) {
      return {taxonomy: m[1], vocabulary: m[2]};
    }

    throw new Error('Invalid field name', name);
  };
};

VariableCriteriaBuilder.prototype = Object.create(AbstractCriteriaBuilder.prototype);

VariableCriteriaBuilder.prototype.visitLeaf = function (node, parentItem) {
  console.log('VariableCriteriaBuilder.visitLeaf');
  var field = node.args[0];
  var values = node.args[1];
  var searchInfo = this.parserFieldName(field);
  var item =
    this.buildLeafItem(searchInfo.taxonomy,
      searchInfo.vocabulary,
      values instanceof Array ? values : [values],
      node,
      parentItem);

  parentItem.children.push(item);
};


angular.module('obiba.mica.search')

  // TODO merge with RqlQueryService or place all node manipularions here
  .service('RqlQueryUtils', [function () {

    /**
     * Finds the parent node to which new queries can be added
     *
     * @param targetNode
     * @returns {*}
     */
    function findValidParentNode(targetNode) {
      var target = targetNode.args.filter(function (query) {
        switch (query.name) {
          case RQL_NODE.AND:
          case RQL_NODE.OR:
          case RQL_NODE.NOT:
          case RQL_NODE.IN:
          case RQL_NODE.OUT:
          case RQL_NODE.EQ:
          case RQL_NODE.GT:
          case RQL_NODE.GE:
          case RQL_NODE.LT:
          case RQL_NODE.LE:
          case RQL_NODE.BETWEEN:
          case RQL_NODE.MATCH:
          case RQL_NODE.EXISTS:
          case RQL_NODE.MISSING:
            return true;
        }

        return false;
      }).pop();

      if (target) {
        return targetNode.args.findIndex(function (arg) {
          return arg.name === target.name;
        });
      }

      return -1;
    }

    this.variableQuery = function () {
      return new RqlQuery(QUERY_TARGETS.VARIABLE);
    };

    this.eqQuery = function (field, term) {
      var query = new RqlQuery(RQL_NODE.EQ);
      query.args.push(term);
      return query;
    };

    this.inQuery = function (field, terms) {
      var hasValues = terms && terms.length > 0;
      var name = hasValues ? RQL_NODE.IN : RQL_NODE.EXISTS;
      var query = new RqlQuery(name);
      query.args.push(field);

      if (hasValues) {
        query.args.push(terms);
      }

      return query;
    };

    this.updateInQuery = function (query, terms, missing) {
      var hasValues = terms && terms.length > 0;
      query.name = hasValues ? RQL_NODE.IN : (missing ? RQL_NODE.MISSING : RQL_NODE.EXISTS);

      if (hasValues) {
        query.args[1] = terms;
      } else {
        query.args.splice(1, 1);
      }

      return query;
    };

    /**
     * Helper finding the vocabulary field, return name if none was found
     *
     * @param vocabulary
     * @returns {*}
     */
    this.vocabularyFieldName = function (vocabulary) {
      var field = vocabulary.attributes.filter(function (attribute) {
        return 'field' === attribute.key;
      }).pop();

      return field ? field.value : vocabulary.name;
    };

    /**
     * Creates a RqlQuery from an item
     *
     * @param item
     * @returns {RqlQuery}
     */
    this.buildRqlQuery = function (item) {

      // TODO take care of other type (min, max, in, ...)
      return this.inQuery(this.vocabularyFieldName(item.vocabulary), item.vocabulary.terms.map(function(term) {
        return term.name;
      }));

    };

    /**
     * Adds a new query to the parent query node
     *
     * @param parentQuery
     * @param query
     * @returns {*}
     */
    this.addQuery = function (parentQuery, query) {

      if (parentQuery.args.length === 0) {
        parentQuery.args.push(query);
      } else {
        var parentIndex = findValidParentNode(parentQuery);

        if (parentIndex === -1) {
          parentQuery.args.push(query);
        } else {
          var oldArg = parentQuery.args.splice(parentIndex, 1).pop();
          var orQuery = new RqlQuery(RQL_NODE.OR);
          orQuery.args.push(oldArg, query);
          parentQuery.args.push(orQuery);
        }
      }

      return parentQuery;
    };

    this.updateQuery = function (query, values, missing) {

      switch (query.name) {
        case RQL_NODE.IN:
        case RQL_NODE.EXISTS:
        case RQL_NODE.MISSING:
          this.updateInQuery(query, values, missing);
          break;
      }

    };

  }])


  .service('RqlQueryService', [
    '$q',
    'TaxonomiesResource',
    'LocalizedValues',
    'RqlQueryUtils',
    function ($q, TaxonomiesResource, LocalizedValues, RqlQueryUtils) {
      var variableTaxonomies = null;
      var studyTaxonomies = null;
      //var networkTaxonomies = null;
      //var datasetTaxonomies = null;

      function isLeafCriteria(item) {
        switch (item.type) {
          case RQL_NODE.IN:
          case RQL_NODE.OUT:
          case RQL_NODE.EQ:
          case RQL_NODE.GT:
          case RQL_NODE.GE:
          case RQL_NODE.LT:
          case RQL_NODE.LE:
          case RQL_NODE.BETWEEN:
          case RQL_NODE.MATCH:
          case RQL_NODE.EXISTS:
          case RQL_NODE.MISSING:
            return true;
        }

        return false;
      }

      function deleteNode(item) {
        var parent = item.parent;
        var query = item.rqlQuery;
        var children = query.args;
        var parentQuery = item.parent.rqlQuery;
        var index = parentQuery.args.indexOf(query);
        if (index === -1) {
          throw new Error('Criteria node not found: ' + item);
        }

        parentQuery.args.splice(index, 1);

        if (children) {
          if (children instanceof Array) {
            parentQuery.args = parentQuery.args.concat(children);
          } else {
            parentQuery.args.push(children);
          }
        }

        if (parent.parent !== null && parentQuery.args.length === 0) {
          deleteNode(parent);
        }
      }

      function deleteNodeCriteriaWithOrphans(item) {
        var parent = item.parent;

        var query = item.rqlQuery;
        var children = query.args;
        var parentQuery = item.parent.rqlQuery;
        var index = parentQuery.args.indexOf(query);
        if (index === -1) {
          throw new Error('Criteria node not found: ' + item);
        }

        parentQuery.args.splice(index, 1);

        if (children) {
          if (children instanceof Array) {
            parentQuery.args = parentQuery.args.concat(children);
          } else {
            parentQuery.args.push(children);
          }
        }

        if (parentQuery.args.length === 0) {
          deleteNode(parent);
        }
      }

      function deleteLeafCriteria(item) {
        var parent = item.parent;
        if (!parent) {
          throw new Error('Cannot remove criteria when parent is NULL');
        }

        var query = item.rqlQuery;
        var parentQuery = item.parent.rqlQuery;
        var index = parentQuery.args.indexOf(query);
        if (index === -1) {
          throw new Error('Criteria node not found: ' + item);
        }

        parentQuery.args.splice(index, 1);

        if ([RQL_NODE.OR, RQL_NODE.AND].indexOf(parent.type) !== -1) {
          deleteNodeCriteriaWithOrphans(parent);
        } else if (parentQuery.args.length === 0) {
          deleteNode(parent);
        }

      }

      /**
       * Removes the item from criteria item tree. This should be from a leaf.
       * @param item
       */
      this.removeCriteriaItem = function (item) {
        if (isLeafCriteria(item)) {
          deleteLeafCriteria(item);
        }
      };

      /**
       * Creates a criteria item
       * @param target
       * @param taxonomy
       * @param vocabulary
       * @param term
       * @param lang
       * @returns A criteria item
       */
      this.createCriteriaItem = function (target, taxonomy, vocabulary, term, lang) {

        return new CriteriaItemBuilder(LocalizedValues, lang)
          .target(target)
          .taxonomy(taxonomy)
          .vocabulary(vocabulary)
          .term(term)
          .build();
      };

      /**
       * Adds new item to the item tree
       *
       * @param rootItem
       * @param item
       */
      this.addCriteriaItem = function (rootRql, newItem) {
        var target = rootRql.args.filter(function (query) {
          return newItem.target === query.name;
        }).pop();

        if (!target) {
          target = new RqlQuery(RQL_NODE[newItem.target.toUpperCase()]);
          rootRql.args.push(target);
        }

        var rqlQuery = RqlQueryUtils.buildRqlQuery(newItem);
        return RqlQueryUtils.addQuery(target, rqlQuery);
      };

      /**
       * Builders registry
       *
       * @type {{variable: builders.variable, study: builders.study}}
       */
      this.builders = {

        /**
         * Builds the Vairable criteria tree
         *
         * @param rootRql
         * @param rootItem
         * @param lang
         * @returns {*}
         */
        variable: function (rootRql, rootItem, lang) {
          var deferred = $q.defer();

          function build(rootRql, rootItem) {
            var builder = new VariableCriteriaBuilder(rootRql, rootItem, variableTaxonomies, LocalizedValues, lang);
            builder.build();
            deferred.resolve(builder.getRootItem());
          }

          if (variableTaxonomies) {
            build(rootRql, rootItem);
          } else {
            TaxonomiesResource.get({
              target: QUERY_TARGETS.VARIABLE
            }).$promise.then(function (response) {
              variableTaxonomies = response;
              build(rootRql, rootItem);
            });
          }

          return deferred.promise;
        },

        /**
         * Builds the Vairable criteria tree
         *
         * @param rootRql
         * @param rootItem
         * @param lang
         * @returns {*}
         */
        study: function (rootRql/*, rootItem, lang*/) {
          var deferred = $q.defer();

          function build(/*node*/) {
            /*
             var builder = new VariableCriteriaBuilder(node, self.variableTaxonomies, LocalizedValues, lang);
             builder.build();*/
            deferred.resolve({}/*builder.getRootItem()*/);
          }

          if (studyTaxonomies) {
            build(rootRql);
          } else {
            TaxonomiesResource.get({
              target: QUERY_TARGETS.STUDY
            }).$promise.then(function (response) {
              studyTaxonomies = response;
              build(rootRql);
            });
          }

          return deferred.promise;
        }
      };

      /**
       * Builds the criteria tree
       *
       * @param rootRql
       * @param lang
       * @returns {*}
       */
      this.createCriteria = function (rootRql, lang) {
        var deferred = $q.defer();
        var rootItem = new CriteriaItemBuilder().type(RQL_NODE.AND).rqlQuery(rootRql).build();

        if (rootRql.args.length === 0) {
          deferred.resolve(rootItem);
          return deferred.promise;
        }

        var queries = [];
        var self = this;
        var resolvedCount = 0;

        rootRql.args.forEach(function (node) {
          if (QUERY_TARGETS[node.name.toUpperCase()]) {
            queries.push(node);
          }
        });

        queries.forEach(function (node) {
          self.builders[node.name](node, rootItem, lang).then(function (item) {
            rootItem.children.push(item);
            resolvedCount++;
            if (resolvedCount === queries.length) {
              deferred.resolve(rootItem);
            }
          });
        });

        return deferred.promise;
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
      this.prepareCriteriaTermsQuery = function (target, query, vocabulary) {
        var parsedQuery = new RqlParser().parse(query);
        var aggregate = new RqlQuery('aggregate');
        var facet = new RqlQuery('facet');
        aggregate.args.push(RqlQueryUtils.vocabularyFieldName(vocabulary));
        parsedQuery.args.some(function (arg) {
          if (arg.name === target) {
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
      this.prepareCoverageQuery = function (query, bucketArgs) {
        var parsedQuery = new RqlParser().parse(query);
        var aggregate = new RqlQuery('aggregate');
        var bucket = new RqlQuery('bucket');
        bucketArgs.forEach(function (b) {
          bucket.args.push(b);
        });
        aggregate.args.push(bucket);
        parsedQuery.args.forEach(function (arg) {
          if (arg.name === 'variable') {
            arg.args.push(aggregate);
          }
        });
        return parsedQuery.serializeArgs(parsedQuery.args);
      };

      this.prepareGraphicsQuery = function (query, aggregateArgs) {
        var parsedQuery = new RqlParser().parse(query);
        // aggregate
        var aggregate = new RqlQuery('aggregate');
        aggregateArgs.forEach(function (a) {
          aggregate.args.push(a);
        });
        // limit
        var limit = new RqlQuery('limit');
        limit.args.push(0);
        limit.args.push(0);
        // study
        var study;
        parsedQuery.args.forEach(function (arg) {
          if (arg.name === 'study') {
            study = arg;
          }
        });
        if (!study) {
          study = new RqlQuery('study');
          parsedQuery.args.push(study);
        }
        study.args.push(aggregate);
        study.args.push(limit);
        // facet
        parsedQuery.args.push(new RqlQuery('facet'));
        return parsedQuery.serializeArgs(parsedQuery.args);
      };

    }]);
