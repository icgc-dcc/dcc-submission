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
package org.icgc.dcc.submission.loader.file;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.loader.record.RecordReader;

import com.google.common.base.Stopwatch;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractFileLoader implements FileLoader {

  /**
   * Dependencies.
   */
  @NonNull
  private final String project;
  @NonNull
  private final String type;
  @NonNull
  protected final RecordReader recordReader;

  /**
   * State.
   */
  private long documentCount;

  @Override
  public void close() throws IOException {
    recordReader.close();
  }

  @Override
  public Void call() throws Exception {
    try {
      beforeLoad();
      log.info("Loading {} of {}", type, project);

      val watch = Stopwatch.createStarted();
      while (recordReader.hasNext()) {
        val record = recordReader.next();
        loadRecord(record);
        documentCount++;
        printStats();
      }

      val elapsed = watch.elapsed(TimeUnit.SECONDS);
      log.info("[{}/{}] Loaded {} document(s). {} docs/sec", project, type, documentCount, getThroughput(elapsed));

      return null;
    } catch (Exception e) {
      log.error("Error loading project %s, type %s", project, type);
      throw e;
    } finally {
      close();
    }
  }

  protected void beforeLoad() {
  }

  abstract protected void loadRecord(Map<String, String> record);

  protected String getName() {
    return project + "/" + type;
  }

  private void printStats() {
    if (documentCount % 10000 == 0) {
      log.info("[{}/{}] {} doc(s) loaded.", project, type, documentCount);
    }
  }

  private long getThroughput(long elapsed) {
    return elapsed == 0L ? documentCount : documentCount / elapsed;
  }

}
