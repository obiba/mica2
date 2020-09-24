<!-- Data access process page macros -->

<!-- Data access process model template -->
<#macro dataAccessProcessModel>
  <div class="card card-info card-outline">
    <div class="card-header">
      <h3 class="card-title"><@message "data-access-process"/></h3>
    </div>
    <div class="card-body">
      <div class="row">
        <div class="col-md-12">
          <div class="timeline">
            <!-- timeline time label -->
            <div class="time-label">
              <span class="bg-secondary">Step 1</span>
            </div>
            <!-- /.timeline-label -->
            <!-- timeline item -->
            <div>
              <i class="fas fa-sign-in-alt bg-secondary"></i>
              <div class="timeline-item">
                <div class="timeline-body">
                  <p class="lead">Create an account</p>
                  <p class="marked">
                    Before initiating a request for data access, all researchers must
                    [create a user account](${contextPath}/signup).
                  </p>
                </div>
              </div>
            </div>
            <!-- END timeline item -->

            <!-- timeline time label -->
            <div class="time-label">
              <span class="bg-info">Step 2</span>
            </div>
            <!-- /.timeline-label -->

            <!-- timeline item -->
            <div>
              <i class="fas fa-edit bg-blue"></i>
              <div class="timeline-item">
                <div class="timeline-body">
                  <p class="lead">Complete and submit your access request form</p>
                  <p class="marked">
                    A new data access requests is to be created from the [<@message "data-access"/>](${contextPath}/data-accesses) page.
                    Researchers are encouraged to fill-in a feasibility form to inquire about the relevance of the project
                    and the requirements involved before submitting an application. When researchers are ready to
                    complete and submit an access request, they must fill and submit an application form online and
                    attach all of the required access documentation.
                  </p>
                  <p class="marked">
                    Following the submission of the application form, the data access committee will review it and will
                    communicate with the researcher when any additional information is needed.
                  </p>
                </div>
              </div>
            </div>
            <!-- END timeline item -->

            <!-- timeline time label -->
            <div class="time-label">
              <span class="bg-blue">Step 3</span>
            </div>
            <!-- /.timeline-label -->
            <!-- timeline item -->
            <div>
              <i class="fas fa-history bg-blue"></i>
              <div class="timeline-item">
                <div class="timeline-body">
                  <p class="lead">Track your request</p>
                  <p class="marked">
                    Researchers will be able to track the progress and history of their access request online, by
                    going to the [<@message "data-access"/>](${contextPath}/data-accesses) page and selecting the
                    data access request. The data access request dashboard summarizes the status of the request and
                    any actions that are required from the researcher. A notification will be sent on approval or
                    rejection of the submitted project.
                  </p>
                </div>
              </div>
            </div>
            <!-- END timeline item -->

            <!-- timeline time label -->
            <div class="time-label">
              <span class="bg-green">Step 4</span>
            </div>
            <!-- /.timeline-label -->
            <!-- timeline item -->
            <div>
              <i class="fas fa-book bg-blue"></i>
              <div class="timeline-item">
                <div class="timeline-body">
                  <p class="lead">Report on project progress</p>
                  <p class="marked">
                    Once the research project has been approved and access to the data is effective, some periodical
                    (yearly) reports can be requested. Some amendments can be added to the original data access request
                    during the progress of the project. A final report will inform the network/study about the
                    scientific achievements and the dissemination of the results of the research project. The information
                    reported will be added to the [<@message "approved-projects"/>](${contextPath}/projects) page.
                  </p>
                </div>
              </div>
            </div>
            <!-- END timeline item -->

            <div>
              <i class="fas fa-circle bg-gray"></i>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</#macro>
