<!DOCTYPE html>
<html lang="${.lang}">
<head>
  <#include "libs/head.ftl">
  <title>${config.name!""} | <@message "test.title"/></title>
</head>
<body class="hold-transition layout-top-nav layout-navbar-fixed">
<div class="app-wrapper d-flex flex-column min-vh-100">

  <!-- Navbar -->
    <#include "libs/top-navbar.ftl">
  <!-- /.navbar -->

  <!-- Content Wrapper. Contains page content -->
 <div class="app-main flex-fill">
    <!-- Content Header (Page header) -->
    <div class="content-header bg-info mb-4">
      <div class="container">
        <div class="row mb-2">
          <div class="col-sm-6">
            <h1 class="m-0"><@message "test.title"/></h1>
          </div><!-- /.col -->
          <div class="col-sm-6">

          </div><!-- /.col -->
        </div><!-- /.row -->
      </div><!-- /.container-fluid -->
    </div>
    <!-- /.content-header -->

    <!-- Main content -->
    <div class="content">
      <div class="container">
        <div class="callout callout-info">
          <p>
            <@message "test.text"/>
          </p>
          <div>
            <dl>
              <dt>.locale</dt>
              <dd>${.locale}</dd>
              <dt>.lang</dt>
              <dd>${.lang}</dd>
              <dt>config.locales</dt>
              <dd><#list config.locales as locale>
                      ${locale.language}
                  </#list></dd>
            </dl>
          </div>
          <div>
            <dl>
              <dt><@message "search.query"/></dt>
              <dd>
                <#if query??>
                  <ul>
                    <#list query?keys as key>
                      <li>${key} = ${query[key]}</li>
                    </#list>
                  </ul>
                </#if>
              </dd>
              <dt>study_taxonomy.vocabulary.methods-design.term.cross_sectional.title</dt>
              <dd>
                <#assign code="study_taxonomy.vocabulary.methods-design.term." + "cross_sectional" + ".title"/>
                <@message code/>
              </dd>
              <dt>hash</dt>
              <dd>
                <#assign myHash = {
                  "name": "mouse",
                  "price": 50,
                  "root": {
                    "level1_1": "value1.1"
                  }
                }>
                <ul>
                  <#list myHash?keys as k>
                    <li>${k}</li>
                </#list>
                  <p>
                    root.level1_1 = ${myHash.root.level1_1}
                  </p>
                </ul>
              </dd>
              <dt>seq</dt>
              <dd>
                  <#assign mySeq = [["1", "a"], ["2", "b"], ["", "c"]]>
                <ul>
                    <#list mySeq as item>
                      <li>${item[0]} : ${item[1]}</li>
                    </#list>
                </ul>
              </dd>
            </dl>
          </div>
        </div>


      </div><!-- /.container-fluid -->
    </div>
    <!-- /.content -->
  </div>
  <!-- /.content-wrapper -->

    <#include "libs/footer.ftl">
</div>
<!-- ./wrapper -->

<#include "libs/scripts.ftl">

</body>
</html>
