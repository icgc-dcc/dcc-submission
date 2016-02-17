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
package org.icgc.dcc.submission.core.report;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static com.google.common.collect.Sets.newTreeSet;
import static org.icgc.dcc.submission.core.report.DataTypeState.getDefaultState;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.common.core.model.DataType;
import org.icgc.dcc.submission.core.util.Serdes.DataTypeDeserializer;
import org.icgc.dcc.submission.core.util.Serdes.DataTypeSerializer;
import org.mongodb.morphia.annotations.Embedded;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;

/**
 * Represents a validation report for a data type within submission within a release.
 * <p>
 * Example:
 * 
 * <pre>
 *  {
 *    "dataType": "DONOR",
 *    "dataTypeState": "NOT_VALIDATED",
 *    "fileTypeReports": [ {
 *      ...
 *    } ]
 *  }
 * </pre>
 */
@Data
@Embedded
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "dataType")
public class DataTypeReport implements ReportElement, Comparable<DataTypeReport> {

  /**
   * Key.
   */
  @JsonSerialize(using = DataTypeSerializer.class)
  @JsonDeserialize(using = DataTypeDeserializer.class)
  private DataType dataType;

  private DataTypeState dataTypeState = getDefaultState();

  private Set<FileTypeReport> fileTypeReports = newTreeSet();

  public DataTypeReport(@NonNull DataType dataType) {
    this.dataType = dataType;
  }

  public DataTypeReport(@NonNull DataTypeReport dataTypeReport) {
    this.dataType = dataTypeReport.dataType;
    this.dataTypeState = dataTypeReport.dataTypeState;

    for (val fileTypeReport : dataTypeReport.fileTypeReports) {
      fileTypeReports.add(new FileTypeReport(fileTypeReport));
    }
  }

  @Override
  public void accept(@NonNull ReportVisitor visitor) {
    // Children first
    for (val fileTypeReport : fileTypeReports) {
      fileTypeReport.accept(visitor);
    }

    // Self last
    visitor.visit(this);
  }

  public void addFileTypeReport(@NonNull FileTypeReport fileTypeReport) {
    fileTypeReports.add(fileTypeReport);
  }

  public void removeFileTypeReport(@NonNull FileTypeReport fileTypeReport) {
    fileTypeReports.remove(fileTypeReport);
  }

  @Override
  public int compareTo(@NonNull DataTypeReport other) {
    return dataType.getId().compareTo(other.dataType.getId());
  }

  public static Set<DataType> getDataTypes(Iterable<DataTypeReport> dataTypeReports) {
    return newLinkedHashSet(Iterables.transform(
        dataTypeReports,
        new Function<DataTypeReport, DataType>() {

          @Override
          public DataType apply(DataTypeReport input) {
            return input.getDataType();
          }

        }));
  }

}
