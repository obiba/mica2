'use strict';

/**
 * Extract variable values frequencies and make a ChartJS settings object of it.
 *
 * @param frequencies
 * @param n
 * @returns
 */
const makeVariableFrequenciesChartSettings = function(frequencies, backgroundColors) {
  const labels = [];
  const dataset = {
    data: [],
    backgroundColor: backgroundColors,
  };
  frequencies.forEach(frequency => {
    if (frequency.count>0) {
      labels.push(frequency.value);
      dataset.data.push(frequency.count);
    }
  });

  return {
    type: 'doughnut',
    data: {
      labels: labels,
      datasets: [dataset]
    },
    options: {
      maintainAspectRatio: false,
      responsive: true,
    }
  };
};

const makeVariableHistogramChartSettings = function(intervalFrequencies, borderColor, backgroundColor) {
  const labels = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10'];
  let dataPoints = [];
  intervalFrequencies.forEach(item => {
    dataPoints.push(item.count);
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
