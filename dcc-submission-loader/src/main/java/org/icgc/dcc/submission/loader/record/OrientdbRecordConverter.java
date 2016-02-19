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
package org.icgc.dcc.submission.loader.record;

import static org.icgc.dcc.submission.loader.util.Fields.PROJECT_ID;

import java.util.Map;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import com.orientechnologies.orient.core.record.impl.ODocument;

@Slf4j
public class OrientdbRecordConverter {

  private final String schemaName;
  private final String project;
  private final ODocument currentDoc;

  public OrientdbRecordConverter(@NonNull String schemaName, @NonNull String project) {
    this.schemaName = schemaName;
    this.project = project;
    this.currentDoc = new ODocument(schemaName);
  }

  public ODocument convert(@NonNull Map<String, String> record) {
    log.debug("Converting record:\n{}", record);

    val document = currentDoc.reset();
    document.setClassName(schemaName);
    for (val entry : record.entrySet()) {
      val fieldName = entry.getKey();
      val fieldValue = entry.getValue();
      document.field(fieldName, fieldValue);
    }

    document.field(PROJECT_ID, project);

    return document;
  }

}
