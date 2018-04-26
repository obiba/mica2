/*
 * Copyright (c) 2018 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.obiba.mica.core;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.AsyncTaskExecutor;

/**
 * Subject aware, exception handling asynchronous task executor.
 */
public class MicaAsyncTaskExecutor implements AsyncTaskExecutor, InitializingBean, DisposableBean {

  private static final Logger log = LoggerFactory.getLogger(MicaAsyncTaskExecutor.class);

  private final AsyncTaskExecutor executor;

  public MicaAsyncTaskExecutor(AsyncTaskExecutor executor) {
    this.executor = executor;
  }

  @Override
  public void execute(Runnable task) {
    Subject subject = SecurityUtils.getSubject();
    Runnable work = subject.associateWith(task);
    executor.execute(work);
  }

  @Override
  public void execute(Runnable task, long startTimeout) {
    Subject subject = SecurityUtils.getSubject();
    Runnable work = subject.associateWith(task);
    executor.execute(createWrappedRunnable(work), startTimeout);
  }

  private <T> Callable<T> createCallable(Callable<T> task) {
    return () -> {
      try {
        return task.call();
      } catch(Exception e) {
        handle(e);
        throw e;
      }
    };
  }

  private Runnable createWrappedRunnable(Runnable task) {
    return () -> {
      try {
        task.run();
      } catch(Exception e) {
        handle(e);
      }
    };
  }

  protected void handle(Exception e) {
    log.error("Caught async exception", e);
  }

  @Override
  public Future<?> submit(Runnable task) {
    return executor.submit(createWrappedRunnable(task));
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    return executor.submit(createCallable(task));
  }

  @Override
  public void destroy() throws Exception {
    if(executor instanceof DisposableBean) {
      DisposableBean bean = (DisposableBean) executor;
      bean.destroy();
    }
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if(executor instanceof InitializingBean) {
      InitializingBean bean = (InitializingBean) executor;
      bean.afterPropertiesSet();
    }
  }
}
