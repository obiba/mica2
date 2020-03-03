<!-- ChartJS -->
<script src="../assets/libs/node_modules/admin-lte/plugins/chart.js/Chart.min.js"></script>
<script>
  $(function () {
    micajs.variable.aggregation('${variable.id}', function(data) {
      $('#loadingSummary').hide();

      if (data.n) {
        $('#n').html(data.total);
        $('#n-values').html(data.n);
        $('#n-missings').html(data.total - data.n);

        $('#counts').show();
      }

      if (data.frequencies) {
        const labels = [];
        const dataset = {
          data: [],
          backgroundColor: ['#f56954', '#00a65a', '#f39c12', '#00c0ef', '#3c8dbc', '#d2d6de',
            '#007bff', '#6610f2','#20c997', '#6f42c1', '#e83e8c', '#dc3545', '#fd7e14', '#ffc107',
            '#28a745',  '#17a2b8'],
        };
        let frequencyRows = '';
        data.frequencies.forEach(frequency => {
          if (frequency.count>0) {
            labels.push(frequency.value);
            dataset.data.push(frequency.count);
          }

          const pct = (frequency.count / data.n) * 100;
          frequencyRows = frequencyRows + '<tr>' +
                  '<td>' + frequency.value + '</td>' +
                  '<td>' + frequency.count + '</td>' +
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
      if (data.statistics) {
        const summary = data.statistics;

        $('#mean').html(summary.n === 0 ? '-' : summary.mean.toFixed(2));
        $('#stdDev').html(summary.n === 0 ? '-' : summary.stdDeviation.toFixed(2));
        $('#variance').html(summary.n === 0 ? '-' : summary.variance.toFixed(2));
        $('#sum').html(summary.n === 0 ? '-' : summary.sum.toFixed(2));
        $('#sum-of-squares').html(summary.n === 0 ? '-' : summary.sumOfSquares.toFixed(2));
        $('#min').html(summary.n === 0 ? '-' : summary.min.toFixed(2));
        $('#max').html(summary.n === 0 ? '-' : summary.max.toFixed(2));

        if (data.intervalFrequencies) {
          const labels = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10'];
          let dataPoints = [];
          data.intervalFrequencies.forEach(item => {
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
      if (!data.frequencies && !data.statistics) {
        $('#noSummary').show();
      }
    });
  });
</script>
