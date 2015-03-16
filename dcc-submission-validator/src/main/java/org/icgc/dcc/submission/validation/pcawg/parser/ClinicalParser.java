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
package org.icgc.dcc.submission.validation.pcawg.parser;

import java.util.List;
import java.util.Map;

import lombok.SneakyThrows;
import lombok.val;

import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.common.hadoop.parser.FileParser;
import org.icgc.dcc.submission.validation.core.Record;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.pcawg.core.Clinical;
import org.icgc.dcc.submission.validation.util.ValidationFileParsers;

import com.google.common.collect.ImmutableList;

/**
 * Parser implementation that creates an in-memory model of clinical data.
 */
public class ClinicalParser {

  public static Clinical parse(ValidationContext context) {
    return new Clinical(
        // Core
        parseFileType(FileType.DONOR_TYPE, context),
        parseFileType(FileType.SPECIMEN_TYPE, context),
        parseFileType(FileType.SAMPLE_TYPE, context),

        // Optional
        parseFileType(FileType.BIOMARKER_TYPE, context),
        parseFileType(FileType.FAMILY_TYPE, context),
        parseFileType(FileType.EXPOSURE_TYPE, context),
        parseFileType(FileType.SURGERY_TYPE, context),
        parseFileType(FileType.THERAPY_TYPE, context));
  }

  @SneakyThrows
  private static List<Record> parseFileType(FileType fileType, ValidationContext context) {
    val fileParser = createParser(fileType, context);

    val records = ImmutableList.<Record> builder();
    for (val file : context.getFiles(fileType)) {
      fileParser.parse(file, (lineNumber, fields) -> {
        val record = new Record(fields, fileType, file, lineNumber);

        records.add(record);
      });

    }

    return records.build();
  }

  private static FileParser<Map<String, String>> createParser(FileType fileType, ValidationContext context) {
    return ValidationFileParsers.newMapFileParser(context, fileType);
  }

}
