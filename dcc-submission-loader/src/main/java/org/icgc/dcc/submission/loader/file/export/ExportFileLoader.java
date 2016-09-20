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
package org.icgc.dcc.submission.loader.file.export;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

import org.icgc.dcc.common.core.util.Joiners;
import org.icgc.dcc.common.core.util.Separators;
import org.icgc.dcc.submission.loader.file.AbstractFileLoader;
import org.icgc.dcc.submission.loader.record.ExportRecordConverter;
import org.icgc.dcc.submission.loader.record.RecordReader;

import com.google.common.base.Joiner;

public class ExportFileLoader extends AbstractFileLoader {

  private static final Joiner JOINER = Joiners.TAB;

  private final BufferedWriter writer;
  private final ExportRecordConverter converter;
  private final List<String> fieldNames;

  @SneakyThrows
  public ExportFileLoader(
      @NonNull String project,
      @NonNull String type,
      @NonNull RecordReader recordReader,
      @NonNull String outputFileName,
      @NonNull ExportRecordConverter converter) {
    super(project, type, recordReader);
    this.converter = converter;
    this.fieldNames = recordReader.getFieldNames();
    this.writer = new BufferedWriter(
        new OutputStreamWriter(
            new GZIPOutputStream(
                new FileOutputStream(
                    new File(outputFileName)))));
  }

  @Override
  @SneakyThrows
  protected void beforeLoad() {
    val header = JOINER.join(fieldNames);
    writer.write(header);
    writer.newLine();
  }

  @Override
  @SneakyThrows
  protected void loadRecord(Map<String, String> record) {
    val convertedRecord = converter.convert(record);
    if (convertedRecord != null) {
      val line = createLine(convertedRecord);
      writer.write(line);
      writer.newLine();
    }
  }

  private String createLine(Map<String, String> record) {
    val stringBuilder = new StringBuilder();
    val fieldNamesSize = fieldNames.size();
    val lastElementIndex = fieldNamesSize - 1;
    for (int i = 0; i < fieldNamesSize; i++) {
      val fieldName = fieldNames.get(i);
      val value = record.get(fieldName);
      stringBuilder.append(value);
      if (i != lastElementIndex) {
        stringBuilder.append(Separators.TAB);
      }
    }

    return stringBuilder.toString();
  }

  @Override
  public void close() throws IOException {
    try {
      super.close();
    } finally {
      writer.close();
    }
  }

}
