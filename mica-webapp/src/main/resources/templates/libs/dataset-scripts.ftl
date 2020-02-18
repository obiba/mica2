<script>
    micajs.stats('datasets', { query: "dataset(in(Mica_dataset.id,${dataset.id}))" }, function(stats) {
        $('#network-hits').text(new Intl.NumberFormat().format(stats.networkResultDto.totalHits));
        $('#study-hits').text(new Intl.NumberFormat().format(stats.studyResultDto.totalHits));
        $('#variable-hits').text(new Intl.NumberFormat().format(stats.variableResultDto.totalHits));
    });
</script>
