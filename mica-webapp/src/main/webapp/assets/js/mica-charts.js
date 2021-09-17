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
      colors: dataset.backgroundColors
    },
    hoverinfo: "label+value",
    values: dataset.data,
    labels
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

  const chartOptions = {
    responsive: true,
    legend: {
      position: 'top',
    },
    title: {
      display: true,
      text: 'Histogram'
    },
    scales: {
      yAxes: [{
        ticks: {
          min: 0,
        }
      }]
    }
  };

  return {
    type: 'bar',
    data: {
      labels: labels,
      datasets: [{
        label: 'Frequencies',
        data: dataPoints,
        barPercentage: 1,
        categoryPercentage: 1,
        borderWidth: 1,
        borderColor: borderColor,
        backgroundColor: backgroundColor
      }]
    },
    options: chartOptions
  };
};

const prepareVariablesClassificationsData = function(chart) {
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

  vocabularies = vocabularies.sort((a, b) => { return ('' + a.label).localeCompare(b.label); });

  return {
    vocabularies: vocabularies,
    itemCounts: itemCounts,
    title: chart.title,
    subtitle: chart.subtitle
  };
};

/**
 * Extract
 *
 * @param chart Data for a taxonomy
 * @param chartDataset which key is used to extract a single bucket
 * @returns ChartJS settings
 */
const makeVariablesClassificationsChartSettings = function(chartData, chartDataset) {
  const names = chartData.vocabularies.map(v => v.name);
  const labels = chartData.vocabularies.map(v => v.label);

  const datasets = [];
  Object.keys(chartData.itemCounts).filter(k => k === chartDataset.key).forEach(k => {
    const dataset = {
      type: "bar",
      orientation: "h",
      marker: {
        color: chartDataset.backgroundColor
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
        ticksuffix: ' '
      }
    }
  };
};
