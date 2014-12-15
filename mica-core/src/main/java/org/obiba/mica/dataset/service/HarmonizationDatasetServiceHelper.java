package org.obiba.mica.dataset.service;

import java.util.concurrent.Future;
import java.util.function.Supplier;

import org.obiba.mica.dataset.domain.DatasetVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

@Component
public class HarmonizationDatasetServiceHelper {

  private static final Logger log = LoggerFactory.getLogger(HarmonizationDatasetService.class);

  @Async
  public Future<Iterable<DatasetVariable>> asyncGetDatasetVariables(Supplier<Iterable<DatasetVariable>> supp) {
    log.info("Getting dataset variables asynchronously.");
    return new AsyncResult<>(supp.get());
  }
}
