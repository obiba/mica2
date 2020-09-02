'use strict';

/**
 * Calculate the chi-squared test.
 */
class ChiSquaredCalculator {

  static compute(chiSquaredInfo) {
    let Chisqcdf = null;
    const Z = chiSquaredInfo.sum;
    const DF = chiSquaredInfo.df;
    if (DF <= 0) {
      // console.log("Degrees of freedom must be positive");
    } else {
      Chisqcdf = this.gammacdf(Z / 2, DF / 2);
    }
    Chisqcdf = Math.round(Chisqcdf * 100000) / 100000;
    return Chisqcdf;
  }

  static logGamma(Z) {
    const S = 1 + 76.18009173 / Z - 86.50532033 / (Z + 1) + 24.01409822 / (Z + 2) - 1.231739516 / (Z + 3) +
      0.00120858003 / (Z + 4) - 0.00000536382 / (Z + 5);
    const LG = (Z - 0.5) * Math.log(Z + 4.5) - (Z + 4.5) + Math.log(S * 2.50662827465);
    return LG;
  }

  // Good for X>A+1.
  static gcf(X, A) {
    let A0 = 0;
    let B0 = 1;
    let A1 = 1;
    let B1 = X;
    let AOLD = 0;
    let N = 0;
    while (Math.abs((A1 - AOLD) / A1) > 0.00001) {
      AOLD = A1;
      N = N + 1;
      A0 = A1 + (N - A) * A0;
      B0 = B1 + (N - A) * B0;
      A1 = X * A0 + N * A1;
      B1 = X * B0 + N * B1;
      A0 = A0 / B1;
      B0 = B0 / B1;
      A1 = A1 / B1;
      B1 = 1;
    }
    const Prob = Math.exp(A * Math.log(X) - X - this.logGamma(A)) * A1;
    return 1 - Prob;
  }

  // Good for X<A+1.
  static gser(X, A) {
    let T9 = 1 / A;
    let G = T9;
    let I = 1;
    while (T9 > G * 0.00001) {
      T9 = T9 * X / (A + I);
      G = G + T9;
      I = I + 1;
    }
    G = G * Math.exp(A * Math.log(X) - X - this.logGamma(A));
    return G;
  }

  static gammacdf(x, a) {
    let GI;
    if (x <= 0) {
      GI = 0;
    } else if (x < a + 1) {
      GI = this.gser(x, a);
    } else {
      GI = this.gcf(x, a);
    }
    return GI;
  }

}

/**
 * Helper class to prepare the crosstab data.
 */
class CollectedDatasetCrosstab {
  constructor(var1, var2, data) {
    this._var1 = var1;
    this._var2 = var2;
    this._data = data;
    this._contingencies = this._normalizeData(data.contingencies ? data.contingencies : [data]);
  }

  getContingencies() {
    return this._contingencies;
  }

  getVariable1() {
    return this._var1;
  }

  getVariable1Href() {
    return Mica.contextPath + '/variable/' + this._var1.id;
  }

  getVariable1Categories() {
    return this._var1.categories ? this._var1.categories : [];
  }

  getVariable2() {
    return this._var2;
  }

  getVariable2Href() {
    return Mica.contextPath + '/variable/' + this._var2.id;
  }

  getVariable2Categories() {
    return this._var2.categories ? this._var2.categories : [];
  }

  _isStatistical(variable) {
    return variable && variable.nature === 'CONTINUOUS';
  }

  /**
   * Retrieves study table info for the result page
   * @param opalTable
   * @returns {{summary: *, population: *, dce: *, project: *, table: *}}
   */
  _extractSummaryInfo(opalTable) {
    var summary = opalTable.studySummary;
    var pop = {};
    var dce = {};
    if (opalTable.studySummary) {
      var studySummary = opalTable.studySummary;
      pop = studySummary.populationSummaries ? studySummary.populationSummaries[0] : null;
      dce = pop && pop.dataCollectionEventSummaries ? pop.dataCollectionEventSummaries.filter(function (dce) {
        return dce.id === opalTable.dataCollectionEventId;
      }) : null;
    }

    let currentLanguage = Mica.locale;

    function extractVariableInfo(variable) {
      let varInfo = {
        label: LocalizedValues.extractLabel(variable.attributes, currentLanguage),
        categories: {}
      };
      let categories = variable.categories;
      if (categories) {
        categories.forEach(cat => {
          varInfo.categories[cat.name] = LocalizedValues.extractLabel(cat.attributes, currentLanguage);
        });
      }
      return varInfo;
    }

    return {
      summary: LocalizedValues.forLang(summary.acronym, currentLanguage),
      population: pop ? LocalizedValues.forLang(pop.name, currentLanguage) : '',
      dce: dce ? LocalizedValues.forLang(dce[0].name, currentLanguage) : '',
      project: opalTable.project,
      table: opalTable.table,
      tableName: LocalizedValues.forLang(opalTable.name, currentLanguage),
      var1: extractVariableInfo(this.getVariable1()),
      var2: extractVariableInfo(this.getVariable2())
    };
  }
  /**
   * Normalized data; fills collection with dummy values (statistical or categorical)
   * @param contingencies
   * @returns {*}
   */
  _normalizeData(contingencies) {
    var v2Cats = this.getVariable2Categories().length > 0 ? this.getVariable2Categories().map(category => category.name) : undefined;
    var v1Cats = this.getVariable1Categories().length > 0 ? this.getVariable1Categories().map(category => category.name) : undefined;

    if (contingencies) {
      const that = this;
      contingencies.forEach(function (contingency) {
        // Show the details anyway.
        contingency.totalPrivacyCheck = contingency.all.n !== -1;
        if (!contingency.totalPrivacyCheck || contingency.all.n > 0) {
          if (that._isStatistical(that.getVariable2())) {
            that._normalizeStatistics(contingency, v1Cats);
          } else {
            that._normalizeFrequencies(contingency, v2Cats);
          }
        }

        if (contingency.studyTable) {
          contingency.info = that._extractSummaryInfo(contingency.studyTable);
        }
      });
    }

    return contingencies;
  }

  _normalizeStatistics(contingency, v1Cats) {
    function createEmptyStatistics() {
      return {
        min: '-',
        max: '-',
        mean: '-',
        stdDeviation: '-'
      };
    }

    contingency.privacyCheck = contingency.aggregations.filter(aggregation => aggregation.statistics !== null).length === contingency.aggregations.length;
    let terms = contingency.aggregations.map(aggregation => aggregation.term);

    if (!contingency.privacyCheck) {
      // server returns no aggregation, create empty ones
      contingency.aggregations.forEach(aggregation => aggregation.statistics = createEmptyStatistics());
      contingency.all.statistics = createEmptyStatistics();
    } else {
      // create the missing category aggregations
      v1Cats.forEach(function (cat, i) {
        if (terms.indexOf(cat) === -1) {
          // create a cat at the same index
          contingency.aggregations.splice(i, 0, {
            n: '-',
            statistics: createEmptyStatistics()
          });
        }
      });
    }
  }

  _normalizeFrequencies(contingency, v2Cats) {

    function percentage(value, total) {
      return total === 0 ? 0 : value / total * 100;
    }

    function expected(cTotal, rTotal, gt) {
      return (cTotal * rTotal) / gt;
    }

    function cellChiSquared(value, expected) {
      return expected === 0 ? 0 : Math.pow(value - expected, 2) / expected;
    }

    function degreeOfFreedom(rows, columns) {
      // should it be absolute value ?
      return (rows - 1) * (columns - 1);
    }

    /**
     * Normalized data; accounts for frequencies with no value (ignored by Elasticsearch)
     * @param aggregation
     */
    function normalize(aggregation) {
      if (!aggregation.frequencies) {
        aggregation.frequencies = [];
      }
      let fCats = aggregation.frequencies.map(function (frq) {
        return frq.value;
      });

      v2Cats.forEach(function (cat, i) {
        if (fCats.indexOf(cat) === -1) {
          // create a cat at the same index
          aggregation.frequencies.splice(i, 0, {
            count: aggregation.privacyCheck ? 0 : '-',
            value: cat
          });
        }
      });
    }

    /**
     * Calculate frequency percentages and chi-squared
     * @param aggregation
     * @param grandTotal
     * @param totals
     * @param chiSquaredInfo
     */
    function statistics(aggregation, grandTotal, totals, chiSquaredInfo) {
      if (chiSquaredInfo) {
        aggregation.percent = percentage(aggregation.n, grandTotal);
        aggregation.frequencies.forEach(function (frequency, i) {
          frequency.percent = percentage(frequency.count, aggregation.n);
          frequency.cpercent = percentage(frequency.count, totals.frequencies[i].count);
          chiSquaredInfo.sum += cellChiSquared(frequency.count, expected(aggregation.n, totals.frequencies[i].count, grandTotal));
        });
      } else {
        aggregation.frequencies.forEach(function (frequency) {
          frequency.percent = percentage(frequency.count, grandTotal);
          frequency.cpercent = percentage(aggregation.n, grandTotal);
        });
      }
    }

    // process contingency
    let privacyThreshold = contingency.privacyThreshold;
    let grandTotal = contingency.all.total;
    contingency.all.privacyCheck = contingency.all.frequencies && contingency.all.frequencies.length > 0;
    normalize(contingency.all, privacyThreshold);
    statistics(contingency.all, grandTotal, contingency.all);

    if (contingency.aggregations) {
      contingency.chiSquaredInfo = {
        pValue: 0,
        sum: 0,
        df: degreeOfFreedom(
          // FIXME var order
          this.getVariable1Categories().length,
          this.getVariable2Categories().length
        )
      };

      contingency.privacyCheck = true;
      contingency.aggregations.forEach(function (aggregation) {
        aggregation.privacyCheck = aggregation.frequencies ? aggregation.frequencies.length > 0 : false;
        contingency.privacyCheck = contingency.privacyCheck && aggregation.privacyCheck;

        normalize(aggregation);
        statistics(aggregation, grandTotal, contingency.all, contingency.chiSquaredInfo);
      });

      if (contingency.privacyCheck) {
        // no cell has an observation < 5
        contingency.chiSquaredInfo.pValue = (1 - ChiSquaredCalculator.compute(contingency.chiSquaredInfo));
      }
    }
  }
}

var initVariableSelectors = function() {
  let options = {
    ajax: {
      url: Mica.contextPath + '/ws/' + Mica.type + '-dataset/' + Mica.dataset + '/variables/_search',
      dataType: 'json',
      delay: 250,
      processResults: function (data, params) {
        params.page = params.page || 1;
        return {
          results: data.variables ? data.variables : [],
          pagination: {
            more: (params.page * 30) < data.total
          }
        };
      },
      cache: true
    },
    minimumInputLength: 1,
    templateResult: formatVariable,
    templateSelection: formatVariableSelection,
    theme: 'bootstrap4'
  };

  // var1 selector
  let options1 = Object.assign({}, options);
  options1.ajax.data = function (params) {
    return {
      query: params.term + '* AND nature:CATEGORICAL', // search term
      page: params.page
    };
  };
  options1.placeholder = 'Select a categorical variable';
  $('#select-var1').select2(options1).on('select2:select', function (e) {
    let data = e.params.data;
    console.log(data);
    Mica.var1 = data;
    refreshCrosstabOnSelection();
  });

  // var2 selector
  let options2 = Object.assign({}, options);
  options2.ajax.data = function (params) {
    return {
      query: params.term + '* AND (nature:CATEGORICAL OR nature:CONTINUOUS)', // search term
      page: params.page
    };
  };
  options2.placeholder = 'Select a variable';
  $('#select-var2').select2(options2).on('select2:select', function (e) {
    let data = e.params.data;
    console.log(data);
    Mica.var2 = data;
    refreshCrosstabOnSelection();
  });

  refreshSubmit();

  function formatVariable(data) {
    if (data.loading) {
      return data.text;
    }

    let element = $('<div><span>' + data.name + '</span><div><small>' + LocalizedValues.extractLabel(data.attributes, Mica.locale) + '</small></div></div>');

    return element;
  }

  function formatVariableSelection(data) {
    return data.name || data.text;
  }

  function refreshCrosstabOnSelection() {
    refreshSubmit();
    if (Mica.var1 && Mica.var2) {
      const var1Name = Mica.var1.name;
      const var2Name = Mica.var2.name;
      window.history.pushState({ var1: var1Name, var2: var2Name }, 'Crosstab ' + var1Name + ' x ' + var2Name,
        Mica.contextPath + '/dataset-crosstab/' + Mica.dataset + '?var1=' + var1Name + '&var2=' + var2Name);
      refreshCrosstab();
    }
  }

  function refreshSubmit() {
    let url = Mica.contextPath + '/dataset-crosstab/' + Mica.dataset;
    let urli = url;
    if (Mica.var1) {
      url = url + '?var1=' + Mica.var1.name;
      urli = urli + '?var2=' + Mica.var1.name;
      if (Mica.var2) {
        url = url + '&var2=' + Mica.var2.name;
        urli = urli + '&var1=' + Mica.var2.name;
      }
    } else if (Mica.var2) {
      url = url + '?var2=' + Mica.var2.name;
      urli = urli + '?var1=' + Mica.var2.name;
    }

    $('#submit').attr('href', url);
    $('#invert').attr('href', urli).attr('disabled', !(Mica.var2 && Mica.var2.nature === 'CATEGORICAL'));
  }

};

const initCrosstabElement = function() {
  $('#loadingCrosstab').show();
  $('#crosstab').hide();
  $('#crosstab > thead').remove();
  $('#crosstab > tbody').remove();
  $('#crosstab > tfoot').remove();
  $('#crosstab').append('<thead></thead>').append('<tbody></tbody>').append('<tfoot></tfoot>');
};

const renderCollectedDatasetCrosstab = function(crosstab) {
  initCrosstabElement();

  const contingency = crosstab.getContingencies()[0];

  // header
  const var2CatsCount = crosstab.getVariable2Categories().length;
  let head = '<th rowspan="2"><a href="' + crosstab.getVariable1Href() + '" target="_blank">' + Mica.var1.name + '</a><div><small>' + contingency.info.var1.label + '</small></div></th>' +
    '<th colspan="' + (var2CatsCount === 0 ? 1 : var2CatsCount) + '"><a href="' + crosstab.getVariable2Href() + '" target="_blank">' + Mica.var2.name + '</a><div><small>' + contingency.info.var2.label + '</small></div></th>' +
    '<th rowspan="2">N</th>';
  const thead = $('#crosstab > thead');
  thead.append('<tr>' + head + '</tr>');
  if (crosstab.getVariable2Categories().length > 0) {
    head = '';
    crosstab.getVariable2Categories().forEach(cat => {
      head = head + '<th>' + cat.name + '<div><small>' + contingency.info.var2.categories[cat.name] + '</small></div></th>';
    });
    thead.append('<tr>' + head + '</tr>');
  }

  // for each category
  contingency.aggregations.forEach(aggregation => {
    let row = '<td>' + aggregation.term + '<div><small>' + contingency.info.var1.categories[aggregation.term] + '</small></div></td>';
    aggregation.frequencies.forEach(freq => {
      row = row + '<td>' + freq.count + ' <small>(' + freq.percent.toFixed(2) + '%)</small>' + '</td>';
    });
    row = row + '<td class="total">' + aggregation.n + '</td>';
    $('#crosstab > tbody').append('<tr>' + row + '</tr>');
  });

  // total
  let row = '<td class="total">N</td>';
  const all = contingency.all;
  all.frequencies.forEach(freq => {
    row = row + '<td class="total">' + freq.count + ' <small>(' + freq.percent.toFixed(2) + '%)</small>' + '</td>';
  });
  row = row + '<td class="total">' + all.n + '</td>';
  $('#crosstab > tbody').append('<tr>' + row + '</tr>');

  // chi-squared test
  const colspan = var2CatsCount + 1;
  let chitest = 'χ² = ' + contingency.chiSquaredInfo.sum.toFixed(4) + ', df = ' + contingency.chiSquaredInfo.df + ', p-value = ' + contingency.chiSquaredInfo.pValue.toFixed(4);
  $('#crosstab > tfoot').append('<tr><td class="total">' + Mica.tr['chi-squared-test'] + '</td><td colspan="' + colspan + '">' + chitest + '</td></tr>')
    .append('<tr><td class="total">' + Mica.tr['n-total'] + '</td><td colspan="' + colspan + '">' + all.total + '</td></tr>');

  $('#crosstab').show();
  $('#loadingCrosstab').hide();
};

const refreshCrosstab = function() {
  $('#results').show();
  if (Mica.type === 'collected') {
    micajs.dataset.datasetContingency(Mica.type, Mica.dataset, Mica.var1.name, Mica.var2.name, function (data) {
      console.log(data);
      // build a data object to make sure that all categories are covered
      const crosstab = new CollectedDatasetCrosstab(Mica.var1, Mica.var2, data);
      console.log(crosstab.getContingencies());
      renderCollectedDatasetCrosstab(crosstab);
    }, function(response) {
      // TODO
      console.log(response);
    });
  }
};

const clearCrosstab = function() {
  $('#results').hide();
  Mica.var1 = undefined;
  Mica.var2 = undefined;
  $('#select-var1').val(null).trigger('change');
  $('#select-var2').val(null).trigger('change');
  $('#submit').attr('href', '#');
  $('#invert').attr('href', '#').attr('disabled', true);
};

$(function () {
  initVariableSelectors();
  if (Mica.var1) {
    $('#select-var1').append(new Option(Mica.var1.name, Mica.var1.name, false, true));
  }
  if (Mica.var2) {
    $('#select-var2').append(new Option(Mica.var2.name, Mica.var2.name, false, true));
  }
  if (Mica.var1 && Mica.var2) {
    refreshCrosstab();
  } else {
    $('#results').hide();
  }
});
