<script>
    $(function () {
        $("#networks").DataTable(dataTablesDefaultOpts);
        $("#individual-studies").DataTable(dataTablesDefaultOpts);
        $("#harmonization-studies").DataTable(dataTablesDefaultOpts);
    });
    micajs.stats('networks', { query: "network(in(Mica_network.id,${network.id}))" }, function(stats) {
        $('#study-hits').text(new Intl.NumberFormat().format(stats.studyResultDto.totalHits));
        $('#dataset-hits').text(new Intl.NumberFormat().format(stats.datasetResultDto.totalHits));
        $('#variable-hits').text(new Intl.NumberFormat().format(stats.variableResultDto.totalHits));
    }, micajs.redirectError);
</script>
