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
package org.icgc.dcc.submission.loader.file.orientdb;

import static org.icgc.dcc.submission.loader.core.DependencyFactory.connect;

import java.io.IOException;
import java.util.Map;

import org.icgc.dcc.submission.loader.file.AbstractFileLoader;
import org.icgc.dcc.submission.loader.record.OrientdbRecordConverter;
import org.icgc.dcc.submission.loader.record.RecordReader;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import lombok.NonNull;
import lombok.val;

public class OrientdbFileLoader extends AbstractFileLoader {

  /**
   * Dependencies.
   */
  private final OrientdbRecordConverter recordConverter;
  private ODatabaseDocumentTx db;

  public OrientdbFileLoader(@NonNull String project, @NonNull String type, @NonNull RecordReader recordReader,
      @NonNull OrientdbRecordConverter recordConverter) {
    super(project, type, recordReader);
    this.recordConverter = recordConverter;
  }

  @Override
  public void close() throws IOException {
    super.close();
    db.close();
  }

  @Override
  protected void beforeLoad() {
    this.db = connect();
    db.activateOnCurrentThread();
  }

  @Override
  protected void loadRecord(Map<String, String> record) {
    val doc = recordConverter.convert(record);
    doc.save();
  }

}
