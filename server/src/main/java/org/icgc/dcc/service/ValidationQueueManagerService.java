/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with 
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT 
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED 
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER 
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.service;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.icgc.dcc.release.ReleaseService;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.validation.ValidationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.inject.Inject;

/**
 * Manages validation queue that:<br>
 * - launches validation for queue submissions<br>
 * - updates submission states upon termination of the validation process
 */
public class ValidationQueueManagerService extends AbstractExecutionThreadService implements ValidationCallback {

  private static final Logger log = LoggerFactory.getLogger(ValidationQueueManagerService.class);

  private static final String SERVICE_NAME = "VQMS";

  private static final int POLLING_FREQUENCY_PER_SEC = 1;

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  private ScheduledFuture<?> scheduleAtFixedRate;

  private final ReleaseService releaseService;

  private final ValidationService validationService;

  private final ValidationCallback thisAsCallback;

  @Inject
  public ValidationQueueManagerService(final ReleaseService releaseService, ValidationService validationService) {

    checkArgument(releaseService != null);
    checkArgument(validationService != null);

    this.releaseService = releaseService;
    this.validationService = validationService;

    this.thisAsCallback = this;
  }

  @Override
  protected String getServiceName() {
    return SERVICE_NAME;
  }

  @Override
  protected void startUp() throws Exception {
    log.info("starting up validation queue service manager");
    super.startUp();
  }

  @Override
  protected void shutDown() throws Exception {
    log.info("shutting down validation queue service manager");
    super.shutDown();
  }

  @Override
  protected void run() throws Exception {

    scheduleAtFixedRate = scheduler.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        if(isRunning()) {
          List<String> queued = releaseService.getQueued();
          log.info("polling every second; queued = {}", queued);
          if(null != queued && queued.isEmpty() == false) {
            String projectKey = queued.get(0);
            Release release = releaseService.getNextRelease().getRelease();
            validationService.validate(release, projectKey, thisAsCallback);
          }
        }
      }
    }, POLLING_FREQUENCY_PER_SEC, POLLING_FREQUENCY_PER_SEC, TimeUnit.SECONDS);

    synchronized(scheduleAtFixedRate) {
      // scheduleAtFixedRate.wait();// TODO: better?
    }
  }

  @Override
  protected void triggerShutdown() {
    boolean cancel = scheduleAtFixedRate.cancel(true);
    log.info("attempt to cancel returned {}", cancel);
    super.triggerShutdown();
  }

  @Override
  public void handleSuccessfulValidation(String projectKey) {
    checkArgument(projectKey != null);
    String dequeuedProjectKey = releaseService.dequeue(true);
    if(dequeuedProjectKey.equals(projectKey) == false) {
      throw new ValidationServiceException(
          String
              .format(
                  "the project key dequeued from the validation queue does not match the expected project key provided: %s != %s",
                  dequeuedProjectKey, projectKey));
    }
  }
}
