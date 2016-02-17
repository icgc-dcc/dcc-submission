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
package org.icgc.dcc.submission.validation.rgv.cli;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.rgv.ReferenceGenomeValidator;
import org.icgc.dcc.submission.validation.rgv.reference.PicardReferenceGenome;

/**
 * Entry point for testing {@link ReferenceGenomeValidator} from the command line in isolation of the other validators
 * and submission system infrastructure.
 */
@Slf4j
public class Main {

  @SneakyThrows
  public static void main(String... args) {
    log.info("Starting reference genome validation...");

    // Resolve configuration @formatter:off
    int i = 0;
    val releaseName = args.length >= ++i ? args[i - 1] : "release2";
    val projectKey  = args.length >= ++i ? args[i - 1] : "project.1";
    val fsRoot      = args.length >= ++i ? args[i - 1] : "/tmp/submission";
    val fsUrl       = args.length >= ++i ? args[i - 1] : "file:///";
    val fastaFile   = args.length >= ++i ? args[i - 1] : "/tmp/GRCh37.fasta";
    // @formatter:on

    val context = getValidationContext(releaseName, projectKey, fsRoot, fsUrl, fastaFile);
    val validator = getValidator(fastaFile);
    validator.validate(context);

    log.info("Finished reference genome validation.");
  }

  private static ValidationContext getValidationContext(String releaseName, String projectKey, String fsRoot,
      String fsUrl, String fastaFile) {
    // @formatter:off
    log.info("releaseName: {}", releaseName);
    log.info("projectKey:  {}", projectKey);
    log.info("fsRoot:      {}", fsRoot);
    log.info("fsUrl:       {}", fsUrl);
    log.info("fastaFile:   {}", fastaFile);
    log.info("input:       {}", fsRoot + "/" + releaseName + "/" + projectKey + "/" );
    log.info("output:      {}", fsRoot + "/" + releaseName + "/" + projectKey + "/" + ".validation");
    // @formatter:on

    return new ReferenceGenomeValidationContext(releaseName, projectKey, fsRoot, fsUrl);
  }

  private static ReferenceGenomeValidator getValidator(String fastaFile) {
    return new ReferenceGenomeValidator(new PicardReferenceGenome(fastaFile));
  }

}
