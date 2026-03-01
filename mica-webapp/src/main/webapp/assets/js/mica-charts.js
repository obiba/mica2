'use strict';

/**
 * Extract variable values frequencies and make a ChartJS settings object of it.
 *
 * @param frequencies
 * @param backgroundColors
 * @param tr
 * @returns Chartjs settings
 */
const makeVariableFrequenciesChartSettings = function(frequencies, backgroundColors, tr) {
  const labels = [];
  const dataset = {
    data: [],
    backgroundColor: backgroundColors,
  };
  frequencies.forEach(frequency => {
    if (frequency.count>0) {
      labels.push(tr[frequency.label] ? tr[frequency.label] : frequency.label);
      dataset.data.push(frequency.count);
    }
  });

  return [{
    type: "pie",
    sort: false,
    hole: 0.5,
    marker: {
      colors: backgroundColors
    },
    hoverinfo: "label+value",
    values: dataset.data,
    labels,
    showlegend: true
  }];
};

/**
 * Make a bar chart from interval frequencies.
 *
 * @param intervalFrequencies
 * @param borderColor
 * @param backgroundColor
 * @returns Chartjs settings
 */
const makeVariableHistogramChartSettings = function(intervalFrequencies, borderColor, backgroundColor) {
  const labels = [];
  let dataPoints = [];
  intervalFrequencies.forEach(item => {
    dataPoints.push(item.count);
    labels.push(item.lower + ' - ' + item.upper);
  });

  return [{
    type: "histogram",
    name: "Frequencies",
    histfunc: "sum",
    x: labels,
    y: dataPoints,
    marker: {
      color: backgroundColor
    }
  }];
};

function prepareVariablesClassificationsData(chart, colorsMap) {
  const itemCounts = {};
  let vocabularies = [];
  chart.data.forEach(vocabularyData => {
    const title = vocabularyData.items[0].title;
    const vocabulary = {
      name: vocabularyData.vocabulary,
      label: title
    };

    vocabularyData.items.filter(item => item.key !== '').forEach(item => {
      if (!itemCounts[item.key]) {
        itemCounts[item.key] = {};
      }
      if (!itemCounts._all) {
        itemCounts._all = {};
      }
      itemCounts[item.key][vocabularyData.vocabulary] = item.value;
      itemCounts._all[vocabularyData.vocabulary] =
        (itemCounts._all[vocabularyData.vocabulary] ? itemCounts._all[vocabularyData.vocabulary] : 0) + item.value;
    });
    
    vocabularies.push(vocabulary);
  });

  return {
    taxonomy: chart.taxonomy,
    vocabularies: vocabularies,
    itemCounts: itemCounts,
    title: chart.title,
    subtitle: chart.subtitle,
    colorsMap: colorsMap
  };
}

/**
 * Extract
 *
 * @param chart Data for a taxonomy
 * @param chartDataset which key is used to extract a single bucket
 * @returns ChartJS settings
 */
function makeVariablesClassificationsChartSettings(chartData, chartDataset) {
  let processedVocabularies = [];
  const names = [];
  const labels = [];
  const colors = [];

  chartData.vocabularies.forEach(v => {
    names.push(v.name);
    labels.push(v.label);

    if (processedVocabularies.indexOf() === -1 && (chartData.colorsMap || {})[chartData.taxonomy]) {
      colors.push(chartData.colorsMap[chartData.taxonomy][v.name]);
      processedVocabularies.push(v.name);
    }
  });

  const datasets = [];
  Object.keys(chartData.itemCounts).filter(k => k === chartDataset.key).forEach(k => {
    const dataset = {
      type: "bar",
      orientation: "h",
      marker: {
        color: (chartData.colorsMap || {})[chartData.taxonomy] && chartDataset.useColorsArray ? colors : chartDataset.backgroundColor
      },
      x: names.map(n => {
        return chartData.itemCounts[k][n] ? chartData.itemCounts[k][n] : 0;
      }),
      y: labels
    };
    datasets.push(dataset);
  });

  return {
    data: datasets,
    layout: {
      yaxis: {
        automargin: true,
        ticksuffix: ' ',
        autorange: 'reversed'
      }
    }
  };
}
