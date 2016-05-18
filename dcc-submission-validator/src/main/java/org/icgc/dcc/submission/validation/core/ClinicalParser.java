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
package org.icgc.dcc.submission.validation.core;

import static org.icgc.dcc.submission.validation.util.ValidationFileParsers.newMapFileParser;

import java.util.List;

import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.submission.core.model.Record;

import com.google.common.collect.Lists;

import lombok.SneakyThrows;
import lombok.val;

/**
 * Parser implementation that creates an in-memory model of clinical data.
 */
public class ClinicalParser {

  public static Clinical parse(ValidationContext context) {
    return new Clinical(
        new ClinicalCore(
            parseFileType(FileType.DONOR_TYPE, context),
            parseFileType(FileType.SPECIMEN_TYPE, context),
            parseFileType(FileType.SAMPLE_TYPE, context)));
  }

  @SneakyThrows
  private static List<Record> parseFileType(FileType fileType, ValidationContext context) {
    val fileParser = newMapFileParser(context, fileType);

    val records = Lists.<Record> newArrayList();
    for (val file : context.getFiles(fileType)) {
      try {
        fileParser.parse(file, (lineNumber, fields) -> {
          Record record = new Record(fields, fileType, file, lineNumber);

          records.add(record);
        });
      } catch (Exception e) {
        throw new IllegalStateException("Failed to parse file " + file, e);
      }
    }

    return records;
  }

}
