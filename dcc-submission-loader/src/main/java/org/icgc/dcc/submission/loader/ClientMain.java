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
package org.icgc.dcc.submission.loader;

import static java.lang.System.err;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.icgc.dcc.submission.loader.core.SubmissionLoader.loadSubmission;
import static org.icgc.dcc.submission.loader.util.Options.verifyIncludeExcludeOptions;

import java.io.IOException;

import org.icgc.dcc.submission.loader.cli.ClientOptions;
import org.icgc.dcc.submission.loader.core.DependencyFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Stopwatch;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientMain {

  public static void main(String... args) throws IOException {
    val options = new ClientOptions();
    val cli = new JCommander(options);

    try {
      cli.parse(args);
      verifyOptions(options);
      log.info("{}", options);

      DependencyFactory.initialize(options);

      try {
        val watch = Stopwatch.createStarted();
        loadSubmission(options);
        log.info("Finished files load in {} second(s).", watch.elapsed(SECONDS));
      } catch (Exception e) {
        log.error("", e);
        System.exit(1);
      } finally {
        DependencyFactory.getInstance().close();
      }
    } catch (ParameterException e) {
      log.warn("", e);
      usage(cli);
    }
  }

  private static void verifyOptions(ClientOptions options) {
    verifyIncludeExcludeOptions(options.excludeFiles, options.includeFiles);
    verifyIncludeExcludeOptions(options.excludeProjects, options.includeProjects);
  }

  private static void usage(JCommander cli) {
    val message = new StringBuilder();
    cli.usage(message);
    err.println(message.toString());
  }

}
