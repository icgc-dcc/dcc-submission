/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.validation.accession.cli;

import org.icgc.dcc.common.ega.client.EGAClient;
import org.icgc.dcc.submission.validation.accession.AccessionValidator;
import org.icgc.dcc.submission.validation.accession.core.AccessionDictionary;
import org.icgc.dcc.submission.validation.accession.ega.EGAFileAccessionValidator;
import org.icgc.dcc.submission.validation.core.BasicValidationContext;
import org.icgc.dcc.submission.validation.core.ValidationContext;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Entry point for testing {@link AccessionValidator} from the command line in isolation of the other validators and
 * submission system infrastructure.
 * <p>
 * Credentials for the {@link EGAClient} must be set through system properties. See javadoc for details.
 */
@Slf4j
public class Main {

  @SneakyThrows
  public static void main(String... args) {
    log.info("Starting accession validation...");

    // Resolve configuration @formatter:off
    int i = 0;
    val releaseName = args.length >= ++i ? args[i - 1] : "release2";
    val projectKey  = args.length >= ++i ? args[i - 1] : "project.1";
    val fsRoot      = args.length >= ++i ? args[i - 1] : "/tmp/submission";
    val fsUrl       = args.length >= ++i ? args[i - 1] : "file:///";
    // @formatter:on

    val context = getValidationContext(releaseName, projectKey, fsRoot, fsUrl);
    val validator = getValidator();
    validator.validate(context);

    log.info("Finished accesssion validation.");
  }

  private static ValidationContext getValidationContext(String releaseName, String projectKey, String fsRoot,
      String fsUrl) {
    // @formatter:off
    log.info("releaseName: {}", releaseName);
    log.info("projectKey:  {}", projectKey);
    log.info("fsRoot:      {}", fsRoot);
    log.info("fsUrl:       {}", fsUrl);
    log.info("input:       {}", fsRoot + "/" + releaseName + "/" + projectKey + "/" );
    log.info("output:      {}", fsRoot + "/" + releaseName + "/" + projectKey + "/" + ".validation");
    // @formatter:on

    return new BasicValidationContext(releaseName, projectKey, fsRoot, fsUrl);
  }

  private static AccessionValidator getValidator() {
    val egaClient = new EGAClient();
    val egaValidator = new EGAFileAccessionValidator(egaClient);

    return new AccessionValidator(new AccessionDictionary(), egaValidator);
  }

}
