/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.validation.sample.parser;

import static org.icgc.dcc.submission.validation.sample.util.SampleTypeFieldNames.SAMPLE_ID_FIELD_NAME;
import static org.icgc.dcc.submission.validation.sample.util.SampleTypeFieldNames.SPECIMEN_ID_FIELD_NAME;
import static org.icgc.dcc.submission.validation.sample.util.SampleTypeFieldNames.SPECIMEN_TYPE_FIELD_NAME;

import java.io.IOException;
import java.util.Map;

import lombok.SneakyThrows;
import lombok.val;

import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.hadoop.parser.FileRecordProcessor;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.sample.core.Samples;
import org.icgc.dcc.submission.validation.sample.util.SampleTypeFileParsers;

import com.google.common.collect.ImmutableMap;

/**
 * Parser implementation that creates an in-memory model of the specimen and sample fields required to perform sample
 * type validation.
 */
public class SamplesParser {

  public static Samples parse(ValidationContext context) {
    val specimen = parseSpecimenMap(context);
    val samples = parseSampleMap(context);
    val innerJoin = innerJoinSpecimenSamples(specimen, samples);

    return new Samples(innerJoin);
  }

  private static Map<String, String> parseSpecimenMap(ValidationContext context) {
    return parseFileTypeMap(FileType.SPECIMEN_TYPE, SPECIMEN_ID_FIELD_NAME, SPECIMEN_TYPE_FIELD_NAME, context);
  }

  private static Map<String, String> parseSampleMap(ValidationContext context) {
    return parseFileTypeMap(FileType.SAMPLE_TYPE, SAMPLE_ID_FIELD_NAME, SPECIMEN_ID_FIELD_NAME, context);
  }

  @SneakyThrows
  private static Map<String, String> parseFileTypeMap(FileType fileType, final String keyFieldName,
      final String valueFieldName, ValidationContext context) {
    val map = ImmutableMap.<String, String> builder();
    val fileParser = SampleTypeFileParsers.newMapFileParser(context, fileType);
    for (val file : context.getFiles(fileType)) {
      fileParser.parse(file, new FileRecordProcessor<Map<String, String>>() {

        @Override
        public void process(long lineNumber, Map<String, String> record) throws IOException {
          val key = record.get(keyFieldName);
          val value = record.get(valueFieldName);

          map.put(key, value);
        }

      });
    }

    return map.build();
  }

  private static Map<String, String> innerJoinSpecimenSamples(Map<String, String> specimen, Map<String, String> samples) {
    val innerJoin = ImmutableMap.<String, String> builder();
    for (val entry : samples.entrySet()) {
      val sampleId = entry.getKey();
      val specimenId = entry.getValue();
      val specimenType = specimen.get(specimenId);

      innerJoin.put(sampleId, specimenType);
    }

    return innerJoin.build();
  }

}
