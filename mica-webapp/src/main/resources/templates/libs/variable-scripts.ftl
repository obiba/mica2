<!-- ChartJS -->
<script src="../assets/libs/node_modules/admin-lte/plugins/chart.js/Chart.min.js"></script>
<script>
  $(function () {
    micajs.variable.summary('${variable.id}', function(data) {
      $('#loadingSummary').hide();

      if (data['Math.CategoricalSummaryDto.categorical']) {
        const labels = [];
        const dataset = {
          data: [],
          backgroundColor: ['#f56954', '#00a65a', '#f39c12', '#00c0ef', '#3c8dbc', '#d2d6de',
            '#007bff', '#6610f2','#20c997', '#6f42c1', '#e83e8c', '#dc3545', '#fd7e14', '#ffc107',
            '#28a745',  '#17a2b8'],
        };
        let frequencyRows = '';
        data['Math.CategoricalSummaryDto.categorical'].frequencies.forEach(frequency => {
          if (frequency.freq>0) {
            labels.push(frequency.value);
            dataset.data.push(frequency.freq);
          }

          const pct = frequency.pct * 100;
          frequencyRows = frequencyRows + '<tr>' +
                  '<td>' + frequency.value + '</td>' +
                  '<td>' + frequency.freq + '</td>' +
                  '<td>' + pct.toFixed(2) + '</td>' +
                  '<td>' + (frequency.missing ? '<i class="fas fa-check"></i>' : '') + '</td>' +
                  '</tr>'
        });
        const chartData = {
          labels: labels,
          datasets: [dataset]
        };
        const chartCanvas = $('#frequencyChart').get(0).getContext('2d');
        const chartOptions = {
          maintainAspectRatio: false,
          responsive: true,
        };
        new Chart(chartCanvas, {
          type: 'doughnut',
          data: chartData,
          options: chartOptions
        });
        $('#frequencyChart').show();

        $('#totalCount').html(data['Math.CategoricalSummaryDto.categorical'].n);
        $('#frequencyValues').html(frequencyRows);

        const dataTableOpts = {
          "paging": false,
          "lengthChange": false,
          "searching": false,
          "ordering": true,
          "order": [[1, "desc"]],
          "info": false,
          "autoWidth": true,
          "language": {
            "url": "../assets/i18n/datatables.${.lang}.json"
          }
        };
        $("#frequencyTable").DataTable(dataTableOpts);

        $('#categoricalSummary').show();
      }
      else if (data['Math.ContinuousSummaryDto.continuous']) {
        const frequencies = data['Math.ContinuousSummaryDto.continuous'].frequencies;
        const summary = data['Math.ContinuousSummaryDto.continuous'].summary;

        const nMissings = frequencies.filter(item => item.missing === true).map(item => item.freq).reduce((a, b) => a + b, 0);

        $('#n').html(summary.n + nMissings);
        $('#n-values').html(summary.n);
        $('#n-missings').html(nMissings);
        $('#mean').html(summary.n === 0 ? '-' : summary.mean.toFixed(2));
        $('#stdDev').html(summary.n === 0 ? '-' : summary.stdDev.toFixed(2));
        $('#median').html(summary.n === 0 ? '-' : summary.median.toFixed(2));
        $('#variance').html(summary.n === 0 ? '-' : summary.variance.toFixed(2));
        $('#min').html(summary.n === 0 ? '-' : summary.min.toFixed(2));
        $('#max').html(summary.n === 0 ? '-' : summary.max.toFixed(2));

        if (summary.n !== 0) {
          const intervalFrequency = data['Math.ContinuousSummaryDto.continuous'].intervalFrequency;
          const min = summary.min.toFixed(2);
          const max = summary.max.toFixed(2);
          const middle = Math.round(min + (max - min)/2);

          const labels = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10'];
          let dataPoints = [];
          intervalFrequency.forEach(item => {
            dataPoints.push(item.freq);
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
                },
                labelString: 'coucou'
              }]
            }
          };

          const chartCanvas = $('#histogramChart').get(0).getContext('2d');
          new Chart(chartCanvas, {
            type: 'bar',
            data: {
              labels: labels,
              datasets: [{
                label: 'Frequencies',
                data: dataPoints,
                barPercentage: 1,
                categoryPercentage: 1,
                borderWidth: 1,
                borderColor: 'rgb(54, 162, 235)',
                backgroundColor: '#3c8dbc'
              }]
            },
            options: chartOptions
          });
          $('#histogramChart').show();
        }

        $('#continuousSummary').show();
      }
      else {
        $('#noSummary').show();
      }
    });
  });
</script>
