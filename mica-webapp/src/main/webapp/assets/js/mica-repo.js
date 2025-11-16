/**
 * Repository related utilities.
 */
'use strict';

/**
 * General query services.
 */
class QueryService {

  /**
   * Get the document type counts.
   *
   * @param type
   * @param params
   * @param onSuccess
   * @param onFailure
   */
  static getCounts(type, params, onSuccess, onFailure) {
    let url = '/ws/' + type + '/_rql';
    if (params && Object.keys(params).length>0) {
      let query = Object.keys(params).map(key => key + '=' + params[key]).join('&');
      url = url + '?' + query;
    }
    axios.get(MicaService.normalizeUrl(url))
      .then(response => {
        if (onSuccess) {
          onSuccess(response.data);
        }
      })
      .catch(response => {
        console.dir(response);
        if (onFailure) {
          onFailure(response);
        }
      });
  }

  static getVariablesCoverage(getCoverageUrl, onsuccess, onfailure) {
    let variableTaxonomiesSearchUrl = '/ws/taxonomies/_filter?target=variable';

    axios.all([axios.get(MicaService.normalizeUrl(getCoverageUrl)), axios.get(MicaService.normalizeUrl(variableTaxonomiesSearchUrl))])
    .then(axios.spread(function (coverageRes, taxoRes) {
      let createColorsMapAndStylesheet = function(colors) {
        let result = {};
        let styleSheet = new CSSStyleSheet();

        if (Array.isArray(taxoRes.data) && Array.isArray(colors)) {
          taxoRes.data.filter(taxo => taxo.name !== 'Mica_variable').forEach(taxo => {
            let index = 0;

            result[taxo.name] = {};

            taxo.vocabularies.forEach(voc => {
              result[taxo.name][voc.name] = colors[index % colors.length];
              styleSheet.insertRule(`.${taxo.name} .${voc.name} { background-color: ${colors[index % colors.length]} }`);
              index = index + 1;
            });
          });
        }

        document.adoptedStyleSheets = [styleSheet];

        return result;
      }

      //console.dir(response);
      if (onsuccess) {
        onsuccess(coverageRes.data, createColorsMapAndStylesheet);
      }

    })).catch(axios.spread(function (coverageRes, taxoRes) {
      console.dir(coverageRes);
      if (onfailure) {
        onfailure(coverageRes, taxoRes);
      }
    }));
  }
}

/**
 * Study utils.
 */
class StudyService {

  /**
   * Find population in study by ID
   * @param study
   */
  static findPopulation(study, id) {
    if (study.populationSummaries) {
      for (const pop of study.populationSummaries) {
        if (pop.id === id) {
          return pop;
        }
      }
    }
    return undefined;
  };

  /**
   * Find DCE in study population by ID
   * @param population
   * @param id
   * @returns {undefined|any}
   */
  static findPopulationDCE(population, id) {
    if (population.dataCollectionEventSummaries) {
      for (const dce of population.dataCollectionEventSummaries) {
        if (dce.id === id) {
          return dce;
        }
      }
    }
    return undefined;
  };

  static getVariablesCoverage(id, taxonomies, lang, onsuccess, onfailure) {
    let url = '/ws/variables/charts/_coverage';
    let query = 'variable(eq(studyId,' + id + '),sort(name),aggregate(re(' + taxonomies.map(tx => tx + '*').join(',') + '),bucket(dceId))),locale(' + lang + ')';
    url = url + '?query=' + query;

    QueryService.getVariablesCoverage(url, onsuccess, onfailure);
  };

}

/**
 * Network utils.
 */
class NetworkService {

  static getVariablesCoverage(id, taxonomies, lang, onsuccess, onfailure) {
    let url = '/ws/variables/charts/_coverage';
    let query = 'network(eq(Mica_network.id,' + id + ')),variable(sort(name),aggregate(re(' + taxonomies.map(tx => tx + '*').join(',') + '),bucket(studyId))),locale(' + lang + ')';
    url = url + '?query=' + query;

    QueryService.getVariablesCoverage(url, onsuccess, onfailure);
  }

  static getAffiliatedMembers(affiliatedMembersQuery, onSuccess, onFailure) {
    const url = '/ws/persons/_search?limit=1000&query=' + affiliatedMembersQuery;

    axios.get(MicaService.normalizeUrl(url))
      .then(response => {
        if (onSuccess) {
          onSuccess(response.data);
        }
      })
      .catch(response => {
        console.dir(response);
        if (onFailure) {
          onFailure(response);
        }
      });
  }
}

/**
 * Variable utils.
 */
class VariableService {

  static getSummary(id, onsuccess, onfailure) {
    let url = '/ws/variable/' + id + '/summary';
    axios.get(MicaService.normalizeUrl(url))
      .then(response => {
        //console.dir(response);
        if (onsuccess) {
          onsuccess(response.data);
        }
      })
      .catch(response => {
        console.dir(response);
        if (onfailure) {
          onfailure(response);
        }
      });
  }

  static getAggregation(id, onsuccess, onfailure) {
    let url = '/ws/variable/' + id + '/aggregation';
    axios.get(MicaService.normalizeUrl(url))
      .then(response => {
        //console.dir(response);
        if (onsuccess) {
          onsuccess(response.data);
        }
      })
      .catch(response => {
        console.dir(response);
        if (onfailure) {
          onfailure(response);
        }
      });
  }

  /**
   * Get the harmonized variables of a Dataschema variable.
   * @param id
   * @param onsuccess
   * @param onfailure
   */
  static getHarmonizations(id, onsuccess, onfailure) {
    let url = '/ws/variable/' + id + '/harmonizations';
    axios.get(MicaService.normalizeUrl(url))
      .then(response => {
        //console.dir(response);
        if (onsuccess) {
          onsuccess(response.data);
        }
      })
      .catch(response => {
        console.dir(response);
        if (onfailure) {
          onfailure(response);
        }
      });
  }

  //
  // Harmo
  //

  static getAttributeValue(variable, namespace, name) {
    if (!variable || !variable.attributes) {
      return undefined;
    }
    for (const attr of variable.attributes) {
      if (attr.namespace === namespace && attr.name === name) {
        return attr.values;
      }
    }
    return undefined;
  };

  /**
   * Get the css class that represents the harmonization status.
   * @param status
   * @returns {string}
   */
  static getHarmoStatusClass(status) {
    let iconClass = 'fas fa-minus text-muted';
    if (status === 'complete') {
      iconClass = 'fas fa-check text-success';
    } else if (status === 'partial') {
      iconClass = 'fas fa-adjust text-muted';
    } else if (status === 'impossible') {
      iconClass = 'fas fa-times text-danger';
    } else if (status === 'undetermined') {
      iconClass = 'fas fa-question text-warning';
    } else if (status === 'na') {
      iconClass = 'fas fa-ban text-black';
    }
    return iconClass;
  };

  static getHarmoStatus(variable) {
    return this.getAttributeValue(variable, 'Mlstr_harmo', 'status');
  };

  static getHarmoStatusDetail(variable) {
    return this.getAttributeValue(variable, 'Mlstr_harmo', 'status_detail');
  };

  static getHarmoComment(variable) {
    return this.getAttributeValue(variable, 'Mlstr_harmo', 'comment');
  };
}

/**
 * Dataset utils.
 */
class DatasetService {

  static getHarmonizedVariables(id, search,  from, limit, onsuccess, onfailure) {
    let url = '/ws/harmonized-dataset/' + id + '/variables/harmonizations/_summary?from=' + from + '&limit=' + limit;
    if (search && search.length > 1) {
      url += `&query=${search.trim()}`;
    }

    axios.get(MicaService.normalizeUrl(url))
      .then(response => {
        //console.dir(response);
        if (onsuccess) {
          onsuccess(response.data);
        }
      })
      .catch(response => {
        console.dir(response);
        if (onfailure) {
          onfailure(response);
        }
      });
  }

  static getContingency(type, id, var1, var2, onsuccess, onfailure) {
    let url = '/ws/' + type.toLowerCase() + '-dataset/' + id + '/variable/' + var1 + '/contingency?by=' + var2;
    axios.get(MicaService.normalizeUrl(url))
      .then(response => {
        //console.dir(response);
        if (onsuccess) {
          onsuccess(response.data);
        }
      })
      .catch(response => {
        console.dir(response);
        if (onfailure) {
          onfailure(response);
        }
      });
  }

  static getVariablesCoverage(id, taxonomies, lang, onsuccess, onfailure) {
    let url = '/ws/variables/charts/_coverage';
    let query = 'variable(eq(datasetId,' + id + '),sort(name),aggregate(re(' + taxonomies.map(tx => tx + '*').join(',') + '),bucket(datasetId))),locale(' + lang + ')';
    url = url + '?query=' + query;

    QueryService.getVariablesCoverage(url, onsuccess, onfailure);
  }

}

class MetricsService {
  static getStats(onsuccess, onfailure) {
    axios.get(MicaService.normalizeUrl('/ws/config/metrics'))
      .then(response => {
        const result = (response.data.documents || []).reduce((acc, obj) => {
          const total = obj.properties.filter(prop => ['total', 'published'].includes(prop.name)).pop();
          acc[obj.type] =  total ? total.value : 0;
          return acc;
        }, {});

        if (onsuccess) {
          onsuccess(result);
        }
      })
      .catch(response => {
        console.dir(response);
        if (onfailure) {
          onfailure(response);
        }
      });
  }
}

