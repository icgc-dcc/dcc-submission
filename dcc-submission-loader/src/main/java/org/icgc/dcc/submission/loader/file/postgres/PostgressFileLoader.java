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
package org.icgc.dcc.submission.loader.file.postgres;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.loader.file.AbstractFileLoader;
import org.icgc.dcc.submission.loader.record.PostgressRecordConverter;
import org.icgc.dcc.submission.loader.record.RecordReader;
import org.icgc.dcc.submission.loader.util.DatabaseFields;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import com.google.common.collect.Lists;

@Slf4j
public class PostgressFileLoader extends AbstractFileLoader {

  private static final int BULK_SIZE = 10_000;

  /**
   * Dependencies.
   */
  private final SimpleJdbcInsert inserter;
  private final PostgressRecordConverter recordConverter;

  /**
   * State.
   */
  private final List<Map<String, Object>> recordBuffer;

  public PostgressFileLoader(@NonNull String project, @NonNull String type, @NonNull RecordReader recordReader,
      @NonNull SimpleJdbcInsert inserter, @NonNull PostgressRecordConverter recordConverter) {
    super(project, type, recordReader);
    this.inserter = inserter;
    this.recordConverter = recordConverter;
    this.recordBuffer = Lists.newLinkedList();
  }

  @Override
  public void close() throws IOException {
    try {
      log.debug("[{}] Flushing records...", getName());
      flushRecords();
    } finally {
      super.close();
    }
  }

  @Override
  protected void loadRecord(Map<String, String> record) {
    val recordWithProject = recordConverter.convert(record);
    val repo = recordWithProject.get("raw_data_repository");
    if (repo != null && (repo.equals("EGA") || repo.equals("1"))) {
      val project = recordWithProject.get(DatabaseFields.PROJECT_ID_FIELD_NAME);
      val accession = recordWithProject.get("raw_data_accession");
      val analysisId = recordWithProject.get("analysis_id");
      System.out.println("OUT - " + project + ":" + analysisId + ":" + accession);
    }
    // recordBuffer.add(recordWithProject);
    //
    // if (isFlushRecords()) {
    // flushRecords();
    // }
  }

  @SuppressWarnings("unchecked")
  private void flushRecords() {
    try {
      inserter.executeBatch(recordBuffer.toArray(new Map[recordBuffer.size()]));
    } catch (Exception e) {
      log.warn("Caught exception while loading records. Will try to reload.");
      sequentiallyReloadRecords();
    }
    recordBuffer.clear();
    log.debug("[{}] Flushed records.", getName());
  }

  private void sequentiallyReloadRecords() {
    int recordsLoaded = 0;
    for (val record : recordBuffer) {
      try {
        inserter.execute(record);
        recordsLoaded++;
      } catch (Exception e) {
        log.error("Failed to load record: {} \n", record, e);
      }
    }

    val total = recordBuffer.size();
    log.info("Loaded: sucessful {}; failed {}; total {} records", recordsLoaded, total - recordsLoaded, total);
  }

  private boolean isFlushRecords() {
    return recordBuffer.size() >= BULK_SIZE;
  }

}
