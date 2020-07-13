<#macro variableSummary variable>
  <img id="loadingSummary" src="${assetsPath}/images/loading.gif">

  <div id="counts" style="display: none;">
    <div class="row">
      <div class="col-xs-12 col-lg-6">
        <dl class="row">
          <dt class="col-sm-4">N</dt>
          <dd class="col-sm-8"><span id="n" class="badge badge-info"></span></dd>
          <dt class="col-sm-4"><@message "n-values"/></dt>
          <dd id="n-values" class="col-sm-8"></dd>
          <dt class="col-sm-4"><@message "n-missings"/></dt>
          <dd id="n-missings" class="col-sm-8"></dd>
        </dl>
      </div>
    </div>
  </div>

  <div id="categoricalSummary" style="display: none" class="border-top mt-3 pt-3">
    <div class="row">
      <div class="col-xs-12 col-lg-6">
        <dl>
          <dt><@message "frequencies"/></dt>
          <dd>
            <table id="frequencyTable" class="table table-striped table-sm">
              <thead>
              <tr>
                <th><@message "value"/></th>
                <th><@message "frequency"/></th>
                <th>%</th>
                <th><@message "missing"/></th>
              </tr>
              </thead>
              <tbody id="frequencyValues">
              </tbody>
            </table>
          </dd>
        </dl>
      </div>
      <div class="col-xs-12 col-lg-6">
        <canvas id="frequencyChart"></canvas>
      </div>
    </div>
  </div>
  <div id="continuousSummary" style="display: none" class="border-top mt-3 pt-3">
    <div class="row">
      <div class="col-xs-12 col-lg-6">
        <dl class="row">
          <dt class="col-sm-4"><@message "mean"/></dt>
          <dd id="mean" class="col-sm-8"></dd>
          <dt class="col-sm-4"><@message "stdDev"/></dt>
          <dd id="stdDev" class="col-sm-8"></dd>
          <dt class="col-sm-4"><@message "sum"/></dt>
          <dd id="sum" class="col-sm-8"></dd>
          <dt class="col-sm-4"><@message "sumOfSquares"/></dt>
          <dd id="sum-of-squares" class="col-sm-8"></dd>
          <dt class="col-sm-4"><@message "variance"/></dt>
          <dd id="variance" class="col-sm-8"></dd>
          <dt class="col-sm-4"><@message "min"/></dt>
          <dd id="min" class="col-sm-8"></dd>
          <dt class="col-sm-4"><@message "max"/></dt>
          <dd id="max" class="col-sm-8"></dd>
        </dl>
      </div>
      <div class="col-xs-12 col-lg-6">
        <canvas id="histogramChart"></canvas>
      </div>
    </div>
  </div>
  <div id="noSummary" style="display: none">
    <span class="text-muted"><@message "no-variable-summary"/></span>
  </div>
</#macro>
