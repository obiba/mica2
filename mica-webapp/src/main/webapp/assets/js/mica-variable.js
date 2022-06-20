<!-- Summary -->
const makeSummary = function(showHarmonizedVariableSummarySelector) {

  const initStudySelector = function(data) {
    let selectStudy = $('#select-study');
    selectStudy.parent().hide();
    if (data.aggregations && data.aggregations.length > 0) {
      selectStudy.parent().show();
      if (selectStudy.children().length === 0) {
        selectStudy.append(new Option(Mica.tr.all, -1, true, true));
        data.aggregations.forEach((agg, i) => {
          const studyTable = agg.studyTable;
          const populationSummary = studyTable.studySummary.populationSummaries.filter(pop => pop.id === studyTable.populationId).pop();
          const dceSummary = populationSummary.dataCollectionEventSummaries ? populationSummary.dataCollectionEventSummaries.filter(dce => dce.id === studyTable.dataCollectionEventId).pop() : undefined;
          let text = LocalizedValues.forLang(studyTable.studySummary.acronym, Mica.currentLanguage) + ' / '
            + LocalizedValues.forLang(populationSummary.name, Mica.currentLanguage)
            + (dceSummary ?  (' / ' + LocalizedValues.forLang(dceSummary.name, Mica.currentLanguage)) : '');
          const tableName = studyTable.name ? LocalizedValues.forLang(studyTable.name, Mica.currentLanguage) : undefined;
          const tableDescription = studyTable.description ? LocalizedValues.forLang(studyTable.description, Mica.currentLanguage) : undefined;
          if (tableName) {
            text = text + ' [' + tableName + (tableDescription ? ': ' + tableDescription : '') + ']';
          } else if (tableDescription) {
            text = text + ' [' + tableDescription + ']';
          }
          selectStudy.append(new Option(text, i, false, false));
        });
      }
      selectStudy.select2({
        theme: 'bootstrap4'
      }).on('select2:select', function (e) {
        let selId = e.params.data.id;
        //console.log(sel);
        if (selId === '-1') {
          renderSummary(Mica.data)
        } else {
          renderSummary(Mica.data.aggregations[Number.parseInt(selId)])
        }
      });
    }
  };

  const renderSummary = function(data) {
    $('#n').html(numberFormatter.format(data.total));
    $('#n-values').html(numberFormatter.format(data.n));
    $('#n-missings').html(numberFormatter.format(data.total - data.n));
    $('#counts').show();

    renderFrequencies(data);
    renderStatistics(data);

    if (!data.frequencies && !data.statistics) {
      $('#noSummary').show();
    }
  };

  const renderFrequencies = function(data) {
    const frequencyChartElem = $('#frequencyChart');
    if (data.frequencies) {
      // frequencies chart
      const padStringWithZeros = (s) => !isNaN(s) ? '0'.repeat(10 - s.length) + s : s;

      const chartData = makeVariableFrequenciesChartSettings(data.frequencies, Mica.backgroundColors, {
        'NOT_NULL': Mica.tr['not-empty-values'],
        'N/A': Mica.tr['empty-values']
      });

      if (frequencyChartElem.length) {
        Plotly.newPlot("frequencyChart", chartData, null, {responsive: true});
        frequencyChartElem.show();
      }

      // frequencies table
      let frequencyRows = '';
      data.frequencies.forEach(frequency => {
        // % over not empty values
        let pctValues = data.n === 0 ? 0 : (frequency.count / data.n) * 100;
        pctValues = numberFormatter.format(pctValues.toFixed(2));
        let pctMissings = data.n === data.total ? 0 : (frequency.count / (data.total - data.n)) * 100;
        pctMissings = numberFormatter.format(pctMissings.toFixed(2));
        if (frequency.missing) {
          pctValues = '';
        } else {
          pctMissings = '';
        }
        let pctTotal = data.total === 0 ? 0 : (frequency.count / data.total) * 100;
        pctTotal = numberFormatter.format(pctTotal.toFixed(2));
        let value = frequency.value;
        try {
          value = numberFormatter.format(frequency.value);
          if (isNaN(value)) {
            value = frequency.value;
          }
        } catch(e) {}
        let valueTxt = Mica.categories[frequency.value] ? Mica.categories[frequency.value] : '';
        if (valueTxt === '') {
          if (value === 'NOT_NULL') {
            value = Mica.tr['not-empty-values'];
            valueTxt = Mica.tr['not-empty-values-description'];
          } else if (value === 'N/A') {
            value = Mica.tr['empty-values'];
            valueTxt = Mica.tr['empty-values-description'];
          }
        }
        frequencyRows = frequencyRows +
          '<tr>' +
          '<td data-sort="' + padStringWithZeros(value) + '">' + value +
          '<p class="text-muted">' + valueTxt + '</p>' + '</td>' +
          '<td data-sort="' + frequency.count + '">' + numberFormatter.format(frequency.count) + '</td>' +
          '<td data-sort="' + pctValues + '">' + pctValues + '</td>' +
          '<td data-sort="' + pctMissings + '">' + pctMissings + '</td>' +
          '<td data-sort="' + pctTotal + '">' + pctTotal + '</td>' +
          '<td>' + (frequency.missing ? '<i class="fas fa-check"></i>' : '') + '</td>' +
          '</tr>';
      });
      $('#frequencyValues').html(frequencyRows);
      $('#categoricalSummary').show();

      if (frequencyChartElem.length) {
        Plotly.relayout("frequencyChart", {width: frequencyChartElem.width(), height: frequencyChartElem.height()});
      }
    } else {
      frequencyChartElem.hide();
      $('#categoricalSummary').hide();
    }
  };

  const renderStatistics = function(data) {
    if (data.statistics) {
      const summary = data.statistics;

      $('#mean').html(summary.n === 0 ? '-' : summary.mean.toFixed(2));
      $('#stdDev').html(summary.n === 0 ? '-' : summary.stdDeviation.toFixed(2));
      $('#variance').html(summary.n === 0 ? '-' : summary.variance.toFixed(2));
      $('#sum').html(summary.n === 0 ? '-' : summary.sum.toFixed(2));
      $('#sum-of-squares').html(summary.n === 0 ? '-' : summary.sumOfSquares.toFixed(2));
      $('#min').html(summary.n === 0 ? '-' : summary.min.toFixed(2));
      $('#max').html(summary.n === 0 ? '-' : summary.max.toFixed(2));

      const histogramChartElem = $('#histogramChart');
      if (data.intervalFrequencies) {
        // histogram chart
        if (histogramChartElem.length) {
          histogramChartElem.show();
          Plotly.newPlot("histogramChart", makeVariableHistogramChartSettings(data.intervalFrequencies, Mica.barChartBorderColor, Mica.barChartBackgroundColor), {title: 'Histogram', width: histogramChartElem.width()});
        }
      } else {
        histogramChartElem.hide();
      }

      $('#continuousSummary').show();
    } else {
      $('#continuousSummary').hide();
    }
  };

  VariableService.getAggregation(Mica.variableId, function (data) {
    Mica.data = data;
    if ('frequencies' in data) {
      data.frequencies.forEach(frequency =>
        frequency.label = Mica.categories[frequency.value] ? `${Mica.categories[frequency.value]} (${frequency.value})` : frequency.value
      )
    }

    $('#loadingSummary').hide();

    if (showHarmonizedVariableSummarySelector && (data.frequencies || data.statistics)) {
      // study tables selector
      initStudySelector(data);
    }
    renderSummary(data);
    $("#frequencyTable").DataTable(Mica.dataTableOpts);
  }, function (data) {
    $('#loadingSummary').hide();
    $('#noSummary').show();
  });

};

<!-- Harmonizations table -->
const makeHarmonizedVariablesTable = function() {
  VariableService.getHarmonizations(Mica.variableId, function(data) {
    $('#loadingHarmonizedVariables').hide();
    const harmonizedVariablesTableBody = $('#harmonizedVariables > tbody');
    if (data.datasetVariableSummaries) {

      const getTableInformation = (study, studyTableName) => {
        const title = localizedString(study.studySummary.acronym) + ' ' + studyTableName;
        let colName = title;

        if (study.description) {
          const description = marked(localizedString(study.description));
          colName =
            '<a href="javascript:void(0)" ' +
            'data-html="true" ' +
            'data-toggle="popover" ' +
            'data-trigger="hover" ' +
            'data-placement="top" ' +
            'data-boundary="viewport" ' +
            'title="'+ Mica.tr['dataset.harmonized-table'] + '"' +
            'data-content="' + description.replaceAll('"', "'") + '">' + title + '</a>';
        }

        return colName;
      };

      for (const harmonizedVariable of data.datasetVariableSummaries) {
        const status = localizedString(VariableService.getHarmoStatus(harmonizedVariable));
        const statusDetail = VariableService.getHarmoStatusDetail(harmonizedVariable);
        const comment = VariableService.getHarmoComment(harmonizedVariable);
        const baseStudyTable = harmonizedVariable.studyTable ? harmonizedVariable.studyTable : harmonizedVariable.harmonizationStudyTable;
        const population = StudyService.findPopulation(baseStudyTable.studySummary, baseStudyTable.populationId);
        const dce = population ? StudyService.findPopulationDCE(population, baseStudyTable.dataCollectionEventId) : undefined;
        const studyAnchor = (summary) => summary.published
          ? '<a href="' + Mica.contextPath + '/study/' + baseStudyTable.studyId + '">' + localizedString(baseStudyTable.studySummary.acronym) + '</a></td>'
          : localizedString(baseStudyTable.studySummary.acronym);
        let dceName = population ? localizedString(population.name) : "";
        const studyTableName =  baseStudyTable.name ? ' (' + localizedString(baseStudyTable.name) + ')' : '';
        if (dce) {
          dceName = dceName + ' -- ' + localizedString(dce.name);
        }
        harmonizedVariablesTableBody.append('<tr>' +
          '<td title=""><a href="' + Mica.contextPath + '/variable/' + harmonizedVariable.resolver.id + '">' + harmonizedVariable.resolver.name + '</a></td>' +
          '<td>' + studyAnchor(baseStudyTable.studySummary) + '</td>' +
          '<td>' + getTableInformation(baseStudyTable, studyTableName) + '</td>' +
          '<td>' + dceName + '</td>' +
          '<td><i class=" ' + VariableService.getHarmoStatusClass(status) + '"></i></td>' +
          '<td>' + localizedString(statusDetail) + '</td>' +
          '<td>' + localizedString(comment) + '</td>' +
          '</tr>')
      }
      $('#harmonizedVariables').show();
      $('[data-toggle="popover"]').popover({delay: { show: 250, hide: 750 }});
    } else {
      $('#noHarmonizedVariables').show();
    }
  }, function (data) {
    $('#loadingHarmonizedVariables').hide();
    $('#noHarmonizedVariables').show();
  });
};
