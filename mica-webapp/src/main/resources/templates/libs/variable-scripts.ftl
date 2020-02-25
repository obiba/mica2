<!-- ChartJS -->
<script src="../assets/libs/node_modules/admin-lte/plugins/chart.js/Chart.min.js"></script>
<script>
  $(function () {
    micajs.variable.summary('${variable.id}', function(data) {
      $('#loadingSummary').hide();

      if (data['Math.CategoricalSummaryDto.categorical']) {
        var labels = [];
        var dataset = {
          data: [],
          backgroundColor : ['#f56954', '#00a65a', '#f39c12', '#00c0ef', '#3c8dbc', '#d2d6de'],
        };
        var frequencyRows = '';
        data['Math.CategoricalSummaryDto.categorical'].frequencies.forEach(frequency => {
          if (frequency.freq>0) {
            labels.push(frequency.value);
            dataset.data.push(frequency.freq);
          }

          var pct = frequency.pct * 100;
          frequencyRows = frequencyRows + '<tr>' +
                  '<td>' + frequency.value + '</td>' +
                  '<td>' + frequency.freq + '</td>' +
                  '<td>' + pct.toFixed(2) + '</td>' +
                  '<td>' + (frequency.missing ? '<i class="fas fa-check"></i>' : '') + '</td>' +
                  '</tr>'
        });
        var chartData = {
          labels: labels,
          datasets: [dataset]
        };
        var chartCanvas = $('#frequencyChart').get(0).getContext('2d');
        var chartOptions     = {
          maintainAspectRatio : false,
          responsive : true,
        };
        new Chart(chartCanvas, {
          type: 'doughnut',
          data: chartData,
          options: chartOptions
        });
        $('#frequencyChart').show();

        $('#totalCount').html(data['Math.CategoricalSummaryDto.categorical'].n);
        $('#frequencyValues').html(frequencyRows);

        var dataTableOpts = {
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
        var summary = data['Math.ContinuousSummaryDto.continuous'].summary;

        $('#n').html(summary.n);
        $('#mean').html(summary.mean.toFixed(2));
        $('#stdDev').html(summary.stdDev.toFixed(2));
        $('#median').html(summary.median.toFixed(2));
        $('#variance').html(summary.variance.toFixed(2));
        $('#min').html(summary.min.toFixed(2));
        $('#max').html(summary.max.toFixed(2));

        $('#continuousSummary').show();
      }
      else {
        $('#noSummary').show();
      }
    });
  });
</script>
