<!-- Summary -->
const makeSummary = function() {

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
      const chartCanvas = frequencyChartElem.get(0).getContext('2d');
      new Chart(chartCanvas, makeVariableFrequenciesChartSettings(data.frequencies, Mica.backgroundColors));
      frequencyChartElem.show();

      // frequencies table
      let frequencyRows = '';
      data.frequencies.forEach(frequency => {
        const pct = data.n === 0 ? 0 : (frequency.count / data.n) * 100;
        frequencyRows = frequencyRows +
          '<tr>' +
          '<td data-sort="' + frequency.value + '">' + numberFormatter.format(frequency.value) +
          '<p class="text-muted">' + (Mica.categories[frequency.value] ? Mica.categories[frequency.value] : '') + '</p>' + '</td>' +
          '<td data-sort="' + frequency.count + '">' + numberFormatter.format(frequency.count) + '</td>' +
          '<td data-sort="' + pct + '">' + numberFormatter.format(pct.toFixed(2)) + '</td>' +
          '<td>' + (frequency.missing ? '<i class="fas fa-check"></i>' : '') + '</td>' +
          '</tr>';
      });
      $('#frequencyValues').html(frequencyRows);
      $('#categoricalSummary').show();
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
        const chartCanvas = histogramChartElem.get(0).getContext('2d');
        new Chart(chartCanvas, makeVariableHistogramChartSettings(data.intervalFrequencies, Mica.barChartBorderColor, Mica.barChartBackgroundColor));
        histogramChartElem.show();
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
    $('#loadingSummary').hide();

    if (data.frequencies || data.statistics) {
      // study tables
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
      for (const harmonizedVariable of data.datasetVariableSummaries) {
        const status = VariableService.getHarmoStatus(harmonizedVariable);
        const statusDetail = VariableService.getHarmoStatusDetail(harmonizedVariable);
        const comment = VariableService.getHarmoComment(harmonizedVariable);
        const baseStudyTable = harmonizedVariable.studyTable ? harmonizedVariable.studyTable : harmonizedVariable.harmonizationStudyTable;
        const population = StudyService.findPopulation(baseStudyTable.studySummary, baseStudyTable.populationId);
        const dce = population ? StudyService.findPopulationDCE(population, baseStudyTable.dataCollectionEventId) : undefined;
        let dceName = population ? localizedString(population.name) : "";
        if (dce) {
          dceName = dceName + ' -- ' + localizedString(dce.name);
        }
        harmonizedVariablesTableBody.append('<tr>' +
          '<td title=""><a href="' + Mica.contextPath + '/variable/' + harmonizedVariable.resolver.id + '">' + harmonizedVariable.resolver.name + '</a> ' + localizedString(baseStudyTable.name) + '' +
          '<div class="text-muted">' + localizedString(baseStudyTable.description) + '</div>' +
          '</td>' +
          '<td><a href="' + Mica.contextPath + '/study/' + baseStudyTable.studyId + '">' + localizedString(baseStudyTable.studySummary.acronym) + '</a></td>' +
          '<td>' + dceName + '</td>' +
          '<td><i class=" ' + VariableService.getHarmoStatusClass(localizedString(status)) + '"></i></td>' +
          '<td>' + localizedString(statusDetail) + '</td>' +
          '<td>' + localizedString(comment) + '</td>' +
          '</tr>')
      }
      $('#harmonizedVariables').show();
    } else {
      $('#noHarmonizedVariables').show();
    }
  }, function (data) {
    $('#loadingHarmonizedVariables').hide();
    $('#noHarmonizedVariables').show();
  });
};
