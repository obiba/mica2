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

/* exported BUCKET_TYPES */
var BUCKET_TYPES = {
  NETWORK: 'network',
  STUDY: 'study',
  DCE: 'dce',
  DATASCHEMA: 'dataschema',
  DATASET: 'dataset'
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
  NAND: 'nand',
  OR: 'or',
  NOR: 'nor',
  NOT: 'not',
  FACET: 'facet',
  LOCALE: 'locale',
  AGGREGATE: 'aggregate',
  BUCKET: 'bucket',

  /* leaf criteria nodes */
  CONTAINS: 'contains',
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

/* exported SORT_FIELDS */
var SORT_FIELDS = {
  ACRONYM: 'acronym',
  NAME: 'name'
};

/* exported targetToType */
function targetToType(target) {
  switch (target.toLocaleString()) {
    case QUERY_TARGETS.NETWORK:
      return QUERY_TYPES.NETWORKS;
    case QUERY_TARGETS.STUDY:
      return QUERY_TYPES.STUDIES;
    case QUERY_TARGETS.DATASET:
      return QUERY_TYPES.DATASETS;
    case QUERY_TARGETS.VARIABLE:
      return QUERY_TYPES.VARIABLES;
  }

  throw new Error('Invalid target: ' + target);
}

/* exported targetToType */
function typeToTarget(type) {
  switch (type.toLocaleString()) {
    case QUERY_TYPES.NETWORKS:
      return QUERY_TARGETS.NETWORK;
    case QUERY_TYPES.STUDIES:
      return QUERY_TARGETS.STUDY;
    case QUERY_TYPES.DATASETS:
      return QUERY_TARGETS.DATASET;
    case QUERY_TYPES.VARIABLES:
      return QUERY_TARGETS.VARIABLE;
  }

  throw new Error('Invalid type: ' + type);
}

/* exported VOCABULARY_TYPES */
var VOCABULARY_TYPES = {
  STRING: 'string',
  INTEGER: 'integer',
  DECIMAL: 'decimal'
};

/* exported CriteriaIdGenerator */
var CriteriaIdGenerator = {
  generate: function (taxonomy, vocabulary, term) {
    return taxonomy && vocabulary ?
    taxonomy.name + '.' + vocabulary.name + (term ? '.' + term.name : '') :
      undefined;
  }
};

/* exported CriteriaItem */
function CriteriaItem(model) {
  var self = this;
  Object.keys(model).forEach(function(k) {
    self[k] = model[k];
  });
}

CriteriaItem.prototype.isRepeatable = function() {
  return false;
};

CriteriaItem.prototype.getTarget = function() {
  return this.target || null;
};

/* exported RepeatableCriteriaItem */
function RepeatableCriteriaItem() {
  CriteriaItem.call(this, {});
  this.list = [];
}

RepeatableCriteriaItem.prototype = Object.create(CriteriaItem.prototype);

RepeatableCriteriaItem.prototype.isRepeatable = function() {
  return true;
};

RepeatableCriteriaItem.prototype.addItem = function(item) {
  this.list.push(item);
  return this;
};

RepeatableCriteriaItem.prototype.items = function() {
  return this.list;
};

RepeatableCriteriaItem.prototype.first = function() {
  return this.list[0];
};

RepeatableCriteriaItem.prototype.getTarget = function() {
  return this.list.length > 0 ? this.list[0].getTarget() : null;
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

  this.selectedTerm = function (term) {
    if (!criteria.selectedTerms) {
      criteria.selectedTerms = [];
    }

    criteria.selectedTerms.push(typeof term === 'string' ? term : term.name);
    return this;
  };

  this.selectedTerms = function (terms) {
    criteria.selectedTerms = terms.filter(function (term) {
      return term;
    }).map(function (term) {
      if (typeof term === 'string') {
        return term;
      } else {
        return term.name;
      }
    });
    return this;
  };

  /**
   * This is
   */
  function prepareForLeaf() {
    if (criteria.term) {
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

    criteria.id = CriteriaIdGenerator.generate(criteria.taxonomy, criteria.vocabulary, criteria.term);
  }

  this.build = function () {
    if (criteria.taxonomy && criteria.vocabulary) {
      prepareForLeaf();
    }
    return new CriteriaItem(criteria);
  };

}

/**
 * Class for all criteria builders
 * @param rootRql
 * @param rootItem
 * @param taxonomies
 * @param LocalizedValues
 * @param lang
 * @constructor
 */
function CriteriaBuilder(rootRql, rootItem, taxonomies, LocalizedValues, lang) {

  /**
   * Helper to get a builder
   * @returns {CriteriaItemBuilder}
   */
  this.newCriteriaItemBuilder = function () {
    return new CriteriaItemBuilder(LocalizedValues, lang);
  };

  this.initialize = function (target) {
    this.leafItemMap = {};
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

    var builder = new CriteriaItemBuilder(self.LocalizedValues, self.lang)
      .type(node.name)
      .target(self.target)
      .taxonomy(targetTaxonomy)
      .vocabulary(targetVocabulary)
      .rqlQuery(node)
      .parent(parentItem);

    builder.selectedTerms(targetTerms).build();

    return builder.build();
  };

}

/**
 * Search for the taxonomy vocabulary corresponding to the provided field name. Can be defined either in the
 * vocabulary field attribute or be the vocabulary name.
 * @param field
 * @returns {{taxonomy: null, vocabulary: null}}
 */
CriteriaBuilder.prototype.fieldToVocabulary = function (field) {
  var found = {
    taxonomy: null,
    vocabulary: null
  };

  var normalizedField = field;
  if (field.indexOf('.') < 0) {
    normalizedField = 'Mica_' + this.target + '.' + field;
  }
  var parts = normalizedField.split('.', 2);
  var targetTaxonomy = parts[0];
  var targetVocabulary = parts[1];

  var foundTaxonomy = this.taxonomies.filter(function (taxonomy) {
    return targetTaxonomy === taxonomy.name;
  });

  if (foundTaxonomy.length === 0) {
    throw new Error('Could not find taxonomy:', targetTaxonomy);
  }

  found.taxonomy = foundTaxonomy[0];

  var foundVocabulary = found.taxonomy.vocabularies.filter(function (vocabulary) {
    return targetVocabulary === vocabulary.name;
  });

  if (foundVocabulary.length === 0) {
    throw new Error('Could not find vocabulary:', targetVocabulary);
  }

  found.vocabulary = foundVocabulary[0];

  return found;
};

/**
 * This method is where a criteria gets created
 */
CriteriaBuilder.prototype.visitLeaf = function (node, parentItem) {
  var match = RQL_NODE.MATCH === node.name;
  var field = node.args[match ? 1 : 0];
  var values = node.args[match ? 0 : 1];

  var searchInfo = this.fieldToVocabulary(field);
  var item =
    this.buildLeafItem(searchInfo.taxonomy,
      searchInfo.vocabulary,
      values instanceof Array ? values : [values],
      node,
      parentItem);

  var current = this.leafItemMap[item.id];

  if (current) {
    if (current.isRepeatable()) {
      current.addItem(item);
    } else {
      console.error('Non-repeatable criteria items must be unique,', current.id, 'will be overwritten.');
      current = item;
    }
  } else {
    current = item.vocabulary.repeatable ? new RepeatableCriteriaItem().addItem(item) : item;
  }

  this.leafItemMap[item.id] = current;

  parentItem.children.push(item);
};

/**
 * Returns all the criterias found
 * @returns {Array}
 */
CriteriaBuilder.prototype.getRootItem = function () {
  return this.rootItem;
};

/**
 * Returns the leaf criteria item map needed for finding duplicates
 * @returns {Array}
 */
CriteriaBuilder.prototype.getLeafItemMap = function () {
  return this.leafItemMap;
};

/**
 * Node condition visitor
 * @param node
 * @param parentItem
 */
CriteriaBuilder.prototype.visitCondition = function (node, parentItem) {
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
CriteriaBuilder.prototype.visitNot = function (node, parentItem) {
  var item = this.newCriteriaItemBuilder().parent(parentItem).rqlQuery(node).type(node.name).build();
  parentItem.children.push(item);

  this.visit(node.args[0], item);
};

/**
 * General purpose node visitor
 * @param node
 * @param parentItem
 */
CriteriaBuilder.prototype.visit = function (node, parentItem) {

  // TODO needs to add more types
  switch (node.name) {
    case RQL_NODE.NOT:
      this.visitNot(node, parentItem);
      break;
    case RQL_NODE.AND:
    case RQL_NODE.NAND:
    case RQL_NODE.OR:
    case RQL_NODE.NOR:
      this.visitCondition(node, parentItem);
      break;

    case RQL_NODE.CONTAINS:
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
    case RQL_NODE.MATCH:
      this.visitLeaf(node, parentItem);
      break;
    default:
  }
};

/**
 * Builds a criteria list for this target
 */
CriteriaBuilder.prototype.build = function () {
  var self = this;
  this.rootRql.args.forEach(function (node) {
    self.visit(node, self.rootItem);
  });
};

angular.module('obiba.mica.search')

  // TODO merge with RqlQueryService or place all node manipularions here
  .service('RqlQueryUtils', [function () {
    var self = this;

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
          case RQL_NODE.NAND:
          case RQL_NODE.OR:
          case RQL_NODE.NOR:
          case RQL_NODE.NOT:
          case RQL_NODE.CONTAINS:
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

    this.vocabularyTermNames = function (vocabulary) {
      return vocabulary && vocabulary.terms ? vocabulary.terms.map(function (term) {
        return term.name;
      }) : [];
    };

    this.hasTargetQuery = function (rootRql, target) {
      return rootRql.args.filter(function (query) {
          switch (query.name) {
            case RQL_NODE.VARIABLE:
            case RQL_NODE.DATASET:
            case RQL_NODE.STUDY:
            case RQL_NODE.NETWORK:
              return target ? target === query.name : true;
            default:
              return false;
          }
        }).length > 0;
    };

    this.variableQuery = function () {
      return new RqlQuery(QUERY_TARGETS.VARIABLE);
    };

    this.eqQuery = function (field, term) {
      var query = new RqlQuery(RQL_NODE.EQ);
      query.args.push(term);
      return query;
    };

    this.orQuery = function (left, right) {
      var query = new RqlQuery(RQL_NODE.OR);
      query.args = [left, right];
      return query;
    };

    this.aggregate = function (fields) {
      var query = new RqlQuery(RQL_NODE.AGGREGATE);
      fields.forEach(function (field) {
        query.args.push(field);
      });
      return query;
    };

    this.limit = function (from, size) {
      var query = new RqlQuery(RQL_NODE.LIMIT);
      query.args.push(from);
      query.args.push(size);
      return query;
    };

    this.fieldQuery = function (name, field, terms) {
      var query = new RqlQuery(name);
      query.args.push(field);

      if (terms && terms.length > 0) {
        query.args.push(terms);
      }

      return query;
    };

    this.inQuery = function (field, terms) {
      var hasValues = terms && terms.length > 0;
      var name = hasValues ? RQL_NODE.IN : RQL_NODE.EXISTS;
      return this.fieldQuery(name, field, terms);
    };

    this.matchQuery = function (field, queryString) {
      var query = new RqlQuery(RQL_NODE.MATCH);
      query.args.push(queryString || '*');
      query.args.push(field);
      return query;
    };

    this.updateMatchQuery = function (query, queryString) {
      query.args[0] = queryString || '*';
      return query;
    };

    this.rangeQuery = function (field, from, to) {
      var query = new RqlQuery(RQL_NODE.BETWEEN);
      query.args.push(field);
      self.updateRangeQuery(query, from, to);
      return query;
    };

    this.updateQueryInternal = function (query, terms) {
      var hasValues = terms && terms.length > 0;

      if (hasValues) {
        query.args[1] = terms;
      } else {
        query.args.splice(1, 1);
      }

      return query;
    };

    this.mergeInQueryArgValues = function (query, terms, replace) {
      var hasValues = terms && terms.length > 0;

      if (hasValues) {
        var current = query.args[1];

        if (!current || replace) {
          query.args[1] = terms;
        } else {
          if (!(current instanceof Array)) {
            current = [current];
          }

          var unique = terms.filter(function (term) {
            return current.indexOf(term) === -1;
          });

          query.args[1] = current.concat(unique);
        }
      } else {
        query.args.splice(1, 1);
      }

      return query;
    };

    this.updateRangeQuery = function (query, from, to, missing) {
      if (missing) {
        query.name = RQL_NODE.MISSING;
        query.args.splice(1, 1);
      } else if (angular.isDefined(from) && from !== null && angular.isDefined(to) && to !== null) {
        query.name = RQL_NODE.BETWEEN;
        query.args[1] = [from, to];
      } else if (angular.isDefined(from) && from !== null) {
        query.name = RQL_NODE.GE;
        query.args[1] = from;
      } else if (angular.isDefined(to) && to !== null) {
        query.name = RQL_NODE.LE;
        query.args[1] = to;
      } else {
        query.name = RQL_NODE.EXISTS;
        query.args.splice(1, 1);
      }
    };

    /**
     * Creates a RqlQuery from an item
     *
     * @param item
     * @returns {RqlQuery}
     */
    this.buildRqlQuery = function (item) {
      if (this.isNumericVocabulary(item.vocabulary)) {
        return this.rangeQuery(this.criteriaId(item.taxonomy, item.vocabulary), null, null);
      } else if (this.isMatchVocabulary(item.vocabulary)) {
        return this.matchQuery(this.criteriaId(item.taxonomy, item.vocabulary), null);
      } else {
        return this.inQuery(
          this.criteriaId(item.taxonomy, item.vocabulary),
          item.term ? item.term.name : undefined
        );
      }
    };

    /**
     * Adds a new query to the parent query node
     *
     * @param parentQuery
     * @param query
     * @returns {*}
     */
    this.addQuery = function (parentQuery, query, logicalOp) {
      if (parentQuery.args.length === 0) {
        parentQuery.args.push(query);
      } else {
        var parentIndex = findValidParentNode(parentQuery);

        if (parentIndex === -1) {
          parentQuery.args.push(query);
        } else {
          var oldArg = parentQuery.args.splice(parentIndex, 1).pop();
          // check if the field is from the target's taxonomy, in which case the criteria is
          // added with a AND operator otherwise it is a OR
          if (!logicalOp && query.args && query.args.length > 0) {
            var targetTaxo = 'Mica_' + parentQuery.name;
            var criteriaVocabulary = query.name === 'match' ? query.args[1] : query.args[0];
            logicalOp = criteriaVocabulary.startsWith(targetTaxo + '.') ? RQL_NODE.AND : RQL_NODE.OR;
          }
          var orQuery = new RqlQuery(logicalOp || RQL_NODE.AND);
          orQuery.args.push(oldArg, query);
          parentQuery.args.push(orQuery);
        }
      }

      return parentQuery;
    };

    /**
     * Update repeatable vocabularies as follows:
     *
     * IN(q, [a,b]) OR [c] => CONTAINS(q, [a,c]) OR CONTAINS(q, [b,c])
     * CONTAINS(q, [a,b]) OR [c] => CONTAINS(q, [a,b,c])
     * EXISTS(q) OR [c] => CONTAINS(q, [c])
     *
     * @param existingItemWrapper
     * @param terms
     */
    this.updateRepeatableQueryArgValues = function (existingItem, terms) {
      var self = this;
      existingItem.items().forEach(function(item) {
        var query = item.rqlQuery;
        switch (query.name) {
          case RQL_NODE.EXISTS:
            query.name = RQL_NODE.CONTAINS;
            self.mergeInQueryArgValues(query, terms, false);
            break;

          case RQL_NODE.CONTAINS:
            self.mergeInQueryArgValues(query, terms, false);
            break;

          case RQL_NODE.IN:
            var values = query.args[1] ?  [].concat(query.args[1]) : [];
            if (values.length === 1) {
              query.name = RQL_NODE.CONTAINS;
              self.mergeInQueryArgValues(query, terms, false);
              break;
            }

            var field = query.args[0];
            var contains = values.filter(function(value){
              // remove duplicates (e.g. CONTAINS(q, [a,a])
              return terms.indexOf(value) < 0;
            }).map(function(value){
              return self.fieldQuery(RQL_NODE.CONTAINS, field, [].concat(value, terms));
            });

            var orRql;
            if (contains.length > 1) {
              var firstTwo = contains.splice(0, 2);
              orRql = self.orQuery(firstTwo[0], firstTwo[1]);

              contains.forEach(function(value){
                orRql = self.orQuery(value, orRql);
              });
              
              query.name = orRql.name;
              query.args = orRql.args;
            } else {
              query.name = RQL_NODE.CONTAINS;
              query.args = contains[0].args;
            }
        }
      });

    };

    this.updateQueryArgValues = function (query, terms, replace) {
      switch (query.name) {
        case RQL_NODE.EXISTS:
        case RQL_NODE.MISSING:
          query.name = RQL_NODE.IN;
          this.mergeInQueryArgValues(query, terms, replace);
          break;
        case RQL_NODE.CONTAINS:
        case RQL_NODE.IN:
          this.mergeInQueryArgValues(query, terms, replace);
          break;
        case RQL_NODE.BETWEEN:
        case RQL_NODE.GE:
        case RQL_NODE.LE:
          query.args[1] = terms;
          break;
        case RQL_NODE.MATCH:
          query.args[0] = terms;
          break;
      }
    };

    this.updateQuery = function (query, values) {
      switch (query.name) {
        case RQL_NODE.CONTAINS:
        case RQL_NODE.IN:
        case RQL_NODE.EXISTS:
        case RQL_NODE.MISSING:
          this.updateQueryInternal(query, values);
          break;
      }
    };

    function vocabularyAttributeValue(vocabulary, key, defaultValue) {
      var value = defaultValue;
      if (vocabulary.attributes) {
        vocabulary.attributes.some(function (attribute) {
          if (attribute.key === key) {
            value = attribute.value;
            return true;
          }

          return false;
        });
      }

      return value;
    }

    this.addLocaleQuery = function (rqlQuery, locale) {
      var found = rqlQuery.args.filter(function (arg) {
        return arg.name === RQL_NODE.LOCALE;
      }).pop();

      if (!found) {
        var localeQuery = new RqlQuery('locale');
        localeQuery.args.push(locale);
        rqlQuery.args.push(localeQuery);
      }
    };

    this.addLimit = function (targetQuery, limitQuery) {
      var found = targetQuery.args.filter(function (arg) {
        return arg.name === RQL_NODE.LIMIT;
      }).pop();

      if (found) {
        found.args = limitQuery.args;
      } else {
        targetQuery.args.push(limitQuery);
      }
    };

    this.addSort = function (targetQuery, sort) {
      var found = targetQuery.args.filter(function (arg) {
        return arg.name === RQL_NODE.SORT;
      }).pop();

      if (!found) {
        var sortQuery = new RqlQuery('sort');
        sortQuery.args.push(sort);
        targetQuery.args.push(sortQuery);
      }
    };

    /**
     * Helper finding the vocabulary field, return name if none was found
     *
     * @param taxonomy
     * @param vocabulary
     * @returns {*}
     */
    this.criteriaId = function (taxonomy, vocabulary) {
      return taxonomy.name + '.' + vocabulary.name;
    };

    this.vocabularyType = function (vocabulary) {
      return vocabularyAttributeValue(vocabulary, 'type', VOCABULARY_TYPES.STRING);
    };

    this.vocabularyField = function (vocabulary) {
      return vocabularyAttributeValue(vocabulary, 'field', vocabulary.name);
    };

    this.vocabularyAlias = function (vocabulary) {
      return vocabularyAttributeValue(vocabulary, 'alias', vocabulary.name);
    };

    this.isTermsVocabulary = function (vocabulary) {
      return self.vocabularyType(vocabulary) === VOCABULARY_TYPES.STRING && vocabulary.terms;
    };

    this.isMatchVocabulary = function (vocabulary) {
      return self.vocabularyType(vocabulary) === VOCABULARY_TYPES.STRING && !vocabulary.terms;
    };

    this.isNumericVocabulary = function (vocabulary) {
      return !vocabulary.terms && (self.vocabularyType(vocabulary) === VOCABULARY_TYPES.INTEGER || self.vocabularyType(vocabulary) === VOCABULARY_TYPES.DECIMAL);
    };

    this.isRangeVocabulary = function (vocabulary) {
      return vocabulary.terms && (self.vocabularyType(vocabulary) === VOCABULARY_TYPES.INTEGER || self.vocabularyType(vocabulary) === VOCABULARY_TYPES.DECIMAL);
    };
  }])

  .service('RqlQueryService', [
    '$q',
    'TaxonomiesResource',
    'TaxonomyResource',
    'LocalizedValues',
    'RqlQueryUtils',
    function ($q, TaxonomiesResource, TaxonomyResource, LocalizedValues, RqlQueryUtils) {
      var taxonomiesCache = {
        variable: null,
        dataset: null,
        study: null,
        network: null
      };

      function findTargetQuery(target, query) {
        return query.args.filter(function (arg) {
          return arg.name === target;
        }).pop();
      }

      function isLeafCriteria(item) {
        switch (item.type) {
          case RQL_NODE.CONTAINS:
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
        var queryArgs = query.args;
        var parentQuery = item.parent.rqlQuery;
        var index = parentQuery.args.indexOf(query);
        var indexChild = parent.children.indexOf(item);

        if (index === -1 || indexChild === -1) {
          throw new Error('Criteria node not found: ' + item);
        }

        parent.children.splice(indexChild, 1);
        item.children.forEach(function (c) {
          c.parent = parent;
        });
        parent.children.splice.apply(parent.children, [indexChild, 0].concat(item.children));

        parentQuery.args.splice(index, 1);

        if (queryArgs) {
          if (queryArgs instanceof Array) {
            parentQuery.args.splice.apply(parentQuery.args, [index, 0].concat(queryArgs));
          } else {
            parentQuery.args.splice(index, 0, queryArgs);
          }
        }

        if (parent.parent !== null && parentQuery.args.length === 0) {
          deleteNode(parent);
        }
      }

      function deleteNodeCriteriaWithOrphans(item) {
        var parent = item.parent;
        var query = item.rqlQuery;
        var queryArgs = query.args;
        var parentQuery = item.parent.rqlQuery;
        var index = parentQuery.args.indexOf(query);
        var indexChild = parent.children.indexOf(item);

        if (index === -1 || indexChild === -1) {
          throw new Error('Criteria node not found: ' + item);
        }

        parent.children.splice(indexChild, 1);
        item.children.forEach(function (c) {
          c.parent = parent;
        });
        parent.children.splice.apply(parent.children, [indexChild, 0].concat(item.children));

        parentQuery.args.splice(index, 1);

        if (queryArgs) {
          if (queryArgs instanceof Array) {
            parentQuery.args.splice.apply(parentQuery.args, [index, 0].concat(queryArgs));
          } else {
            parentQuery.args.splice(index, 0, queryArgs);
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

        if ([RQL_NODE.OR, RQL_NODE.AND, RQL_NODE.NAND, RQL_NODE.NOR].indexOf(parent.type) !== -1) {
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
        } else {
          deleteNode(item);
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
        function createBuilder(taxonomy, vocabulary, term) {
          return new CriteriaItemBuilder(LocalizedValues, lang)
            .target(target)
            .taxonomy(taxonomy)
            .vocabulary(vocabulary)
            .term(term);
        }

        if (angular.isString(taxonomy)) {
          return TaxonomyResource.get({
            target: target,
            taxonomy: taxonomy
          }).$promise.then(function (taxonomy) {
            vocabulary = taxonomy.vocabularies.filter(function (v) {return v.name === vocabulary; })[0];
            term = vocabulary.terms.filter(function (t) {return t.name === term; })[0];

            return createBuilder(taxonomy, vocabulary, term).build();
          });
        }

        return createBuilder(taxonomy, vocabulary, term).build();
      };

      /**
       * Adds new item to the item tree
       *
       * @param rootItem
       * @param item
       */
      this.addCriteriaItem = function (rootRql, newItem, logicalOp) {
        var target = rootRql.args.filter(function (query) {
          return newItem.target === query.name;
        }).pop();

        if (!target) {
          target = new RqlQuery(RQL_NODE[newItem.target.toUpperCase()]);
          rootRql.args.push(target);
        }

        var rqlQuery = newItem.rqlQuery ? newItem.rqlQuery : RqlQueryUtils.buildRqlQuery(newItem);
        return RqlQueryUtils.addQuery(target, rqlQuery, logicalOp);
      };

      /**
       * Update an existing item to the item tree
       *
       * @param rootItem
       * @param item
       */
      this.updateCriteriaItem = function (existingItem, newItem, replace) {
        var newTerms;
        var isRepeatable = existingItem.isRepeatable();
        var isMatchNode = !isRepeatable && existingItem.rqlQuery.name === RQL_NODE.MATCH;

        if(replace && newItem.rqlQuery) {
          existingItem.rqlQuery.name = newItem.rqlQuery.name;
        }
        
        if (newItem.rqlQuery) {
          newTerms = newItem.rqlQuery.args[isMatchNode ? 0 : 1];
        } else if (newItem.term) {
          newTerms = [newItem.term.name];
        } else {
          existingItem = isRepeatable ? existingItem.first() : existingItem;
          existingItem.rqlQuery.name = RQL_NODE.EXISTS;
          existingItem.rqlQuery.args.splice(1, 1);
        }

        if (newTerms) {
          if (isRepeatable) {
            RqlQueryUtils.updateRepeatableQueryArgValues(existingItem, newTerms);
          } else {
            RqlQueryUtils.updateQueryArgValues(existingItem.rqlQuery, newTerms, replace);
          }
        }
      };

      /**
       * Builders registry
       *
       * @type {{variable: builders.variable, study: builders.study}}
       */
      this.builders = function (target, rootRql, rootItem, lang) {
        var deferred = $q.defer();

        function build(rootRql, rootItem) {
          var builder = new CriteriaBuilder(rootRql, rootItem, taxonomiesCache[target], LocalizedValues, lang);
          builder.initialize(target);
          builder.build();
          deferred.resolve({root: builder.getRootItem(), map: builder.getLeafItemMap()});
        }

        if (taxonomiesCache[target]) {
          build(rootRql, rootItem);
        } else {
          TaxonomiesResource.get({
            target: target
          }).$promise.then(function (response) {
            taxonomiesCache[target] = response;
            build(rootRql, rootItem);
          });
        }

        return deferred.promise;
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
        var leafItemMap = {};

        if (!RqlQueryUtils.hasTargetQuery(rootRql)) {
          deferred.resolve({root: rootItem, map: leafItemMap});
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
          self.builders(node.name, node, rootItem, lang).then(function (result) {
            rootItem.children.push(result.root);
            leafItemMap = angular.extend(leafItemMap, result.map);
            resolvedCount++;
            if (resolvedCount === queries.length) {
              deferred.resolve({root: rootItem, map: leafItemMap});
            }
          });
        });

        return deferred.promise;
      };

      /**
       * Append the aggregate and facet for criteria term listing.
       *
       * @param query
       * @para
       * @returns the new query
       */
      this.prepareCriteriaTermsQuery = function (query, item, lang) {
        function iterReplaceQuery(query, criteriaId, newQuery) {
          if (!query || !query.args) {
            return null;
          }

          if ((query.name === RQL_NODE.IN || query.name === RQL_NODE.MISSING || query.name === RQL_NODE.CONTAINS) && query.args[0] === criteriaId) {
            return query;
          }

          for (var i = query.args.length; i--;) {
            var res = iterReplaceQuery(query.args[i], criteriaId, newQuery);

            if (res) {
              query.args[i] = newQuery;
            }
          }
        }

        var parsedQuery = new RqlParser().parse(query);
        var targetQuery = parsedQuery.args.filter(function (node) {
          return node.name === item.target;
        }).pop();

        if (targetQuery) {
          var anyQuery = new RqlQuery(RQL_NODE.EXISTS),
            criteriaId = RqlQueryUtils.criteriaId(item.taxonomy, item.vocabulary);

          anyQuery.args.push(criteriaId);
          iterReplaceQuery(targetQuery, criteriaId, anyQuery);
          targetQuery.args.push(RqlQueryUtils.aggregate([criteriaId]));
          targetQuery.args.push(RqlQueryUtils.limit(0, 0));
        }

        parsedQuery.args.push(new RqlQuery(RQL_NODE.FACET));

        if (lang) {
          RqlQueryUtils.addLocaleQuery(parsedQuery, lang);
        }

        return parsedQuery.serializeArgs(parsedQuery.args);
      };

      this.prepareSearchQuery = function (type, query, pagination, lang, sort) {
        var rqlQuery = angular.copy(query);
        var target = typeToTarget(type);
        RqlQueryUtils.addLocaleQuery(rqlQuery, lang);
        var targetQuery = findTargetQuery(target, rqlQuery);

        if (!targetQuery) {
          targetQuery = new RqlQuery(target);
          rqlQuery.args.push(targetQuery);
        }

        var limit = pagination[target] || {from: 0, size: 10};
        RqlQueryUtils.addLimit(targetQuery, RqlQueryUtils.limit(limit.from, limit.size));

        if (sort) {
          RqlQueryUtils.addSort(targetQuery, sort);
        }

        return new RqlQuery().serializeArgs(rqlQuery.args);
      };

      /**
       * Append the aggregate and bucket operations to the variable.
       *
       * @param query
       * @param bucketArg
       * @returns the new query
       */
      this.prepareCoverageQuery = function (query, bucketArg) {
        var parsedQuery = new RqlParser().parse(query);
        var aggregate = new RqlQuery('aggregate');
        var bucketField;

        switch (bucketArg) {
          case BUCKET_TYPES.NETWORK:
            bucketField = 'networkId';
            break;
          case BUCKET_TYPES.STUDY:
            bucketField = 'studyIds';
            break;
          case BUCKET_TYPES.DCE:
            bucketField = 'dceIds';
            break;
          case BUCKET_TYPES.DATASCHEMA:
          case BUCKET_TYPES.DATASET:
            bucketField = 'datasetId';
            break;
        }

        var bucket = new RqlQuery('bucket');
        bucket.args.push(bucketField);
        aggregate.args.push(bucket);

        var variable;
        parsedQuery.args.forEach(function (arg) {
          if (!variable && arg.name === 'variable') {
            variable = arg;
          }
        });
        if (!variable) {
          variable = new RqlQuery('variable');
          parsedQuery.args.push(variable);
        }

        if (variable.args.length > 0 && variable.args[0].name !== 'limit') {
          var variableType = new RqlQuery('in');
          variableType.args.push('Mica_variable.variableType');
          if (bucketArg === BUCKET_TYPES.NETWORK || bucketArg === BUCKET_TYPES.DATASCHEMA) {
            variableType.args.push('Dataschema');
          } else {
            variableType.args.push('Study');
          }
          var andVariableType = new RqlQuery('and');
          andVariableType.args.push(variableType);
          andVariableType.args.push(variable.args[0]);
          variable.args[0] = andVariableType;
        }

        variable.args.push(aggregate);
        return parsedQuery.serializeArgs(parsedQuery.args);
      };

      this.prepareGraphicsQuery = function (query, aggregateArgs, bucketArgs) {
        var parsedQuery = new RqlParser().parse(query);
        // aggregate
        var aggregate = new RqlQuery(RQL_NODE.AGGREGATE);
        aggregateArgs.forEach(function (a) {
          aggregate.args.push(a);
        });
        //bucket
        if (bucketArgs && bucketArgs.length > 0) {
          var bucket = new RqlQuery(RQL_NODE.BUCKET);
          bucketArgs.forEach(function (b) {
            bucket.args.push(b);
          });
          aggregate.args.push(bucket);
        }

        // study
        var study;
        var hasQuery = false;
        var hasStudyTarget = false;
        parsedQuery.args.forEach(function (arg) {
          if (arg.name === 'study') {
            hasStudyTarget = true;
            var limitIndex = null;
            hasQuery = arg.args.filter(function (requestArg, index) {
              if (requestArg.name === 'limit') {
                limitIndex = index;
              }
              return ['limit', 'sort', 'aggregate'].indexOf(requestArg.name) < 0;
            }).length;
            if (limitIndex !== null) {
              arg.args.splice(limitIndex, 1);
            }
            study = arg;
          }
        });
        // Study match all if no study query.
        if (!hasStudyTarget) {
          study = new RqlQuery('study');
          parsedQuery.args.push(study);
        }
        if (!hasQuery) {
          study.args.push(new RqlQuery(RQL_NODE.MATCH));
        }
        study.args.push(aggregate);
        // facet
        parsedQuery.args.push(new RqlQuery('facet'));
        return parsedQuery.serializeArgs(parsedQuery.args);
      };

      this.getTargetAggregations = function (joinQueryResponse, criterion, lang) {

        /**
         * Helper to merge the terms that are not in the aggregation list
         *
         * @param aggs
         * @param vocabulary
         * @returns Array of aggs
         */
        function addMissingTerms(aggs, vocabulary) {
          var terms = vocabulary.terms;
          if (terms && terms.length > 0) {
            var keys = aggs && aggs.map(function (agg) {
                return agg.key;
              }) || [];

            if (aggs) {
              // Add the missing terms not present in the aggs list
              var missingTerms = [];

              terms.forEach(function (term) {
                if (keys.length === 0 || keys.indexOf(term.name) === -1) {
                  missingTerms.push({
                    count: 0,
                    default: 0,
                    description: LocalizedValues.forLocale(term.description, lang),
                    key: term.name,
                    title: LocalizedValues.forLocale(term.title, lang)
                  });
                }
              });

              return aggs.concat(missingTerms);
            }

            // The query didn't have any match, return default empty aggs based on the vocabulary terms
            return terms.map(function (term) {
              return {
                count: 0,
                default: 0,
                description: LocalizedValues.forLocale(term.description, lang),
                key: term.name,
                title: LocalizedValues.forLocale(term.title, lang)
              };
            });

          }

          return aggs;
        }

        function getChildAggragations(parentAgg, aggKey) {
          if (parentAgg.children) {
            var child = parentAgg.children.filter(function (child) {
              return child.hasOwnProperty(aggKey);
            }).pop();

            if (child) {
              return child[aggKey];
            }
          }

          return null;
        }

        var alias = RqlQueryUtils.vocabularyAlias(criterion.vocabulary);
        var targetResponse = joinQueryResponse[criterion.target + 'ResultDto'];

        if (targetResponse && targetResponse.aggs) {
          var isProperty = criterion.taxonomy.name.startsWith('Mica_');
          var filter = isProperty ? alias : criterion.taxonomy.name;
          var filteredAgg = targetResponse.aggs.filter(function (agg) {
            return agg.aggregation === filter;
          }).pop();

          if (filteredAgg) {
            if (isProperty) {
              if (RqlQueryUtils.isNumericVocabulary(criterion.vocabulary)) {
                return filteredAgg['obiba.mica.StatsAggregationResultDto.stats'];
              } else {
                return RqlQueryUtils.isRangeVocabulary(criterion.vocabulary) ?
                  addMissingTerms(filteredAgg['obiba.mica.RangeAggregationResultDto.ranges'], criterion.vocabulary) :
                  addMissingTerms(filteredAgg['obiba.mica.TermsAggregationResultDto.terms'], criterion.vocabulary);
              }
            } else {
              var vocabularyAgg = filteredAgg.children.filter(function (agg) {
                return agg.aggregation === alias;
              }).pop();

              if (vocabularyAgg) {
                return RqlQueryUtils.isRangeVocabulary(criterion.vocabulary) ?
                  addMissingTerms(getChildAggragations(filteredAgg, 'obiba.mica.RangeAggregationResultDto.ranges'), criterion.vocabulary) :
                  addMissingTerms(getChildAggragations(filteredAgg, 'obiba.mica.TermsAggregationResultDto.terms'), criterion.vocabulary);
              }
            }
          }
        }

        return addMissingTerms([], criterion.vocabulary);
      };

      this.findCriterion = function(criteria, id) {
        function inner(criteria, id) {
          var result;
          if(criteria.id === id) { return criteria; }
          for(var i = criteria.children.length; i--;){
            result = inner(criteria.children[i], id);

            if (result) {return result;}
          }
        }
        
        return inner(criteria, id);
      };
    }]);
