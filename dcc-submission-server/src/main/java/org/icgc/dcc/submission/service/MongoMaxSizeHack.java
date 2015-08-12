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
package org.icgc.dcc.submission.service;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;
import static org.icgc.dcc.submission.core.report.ErrorParameterKey.DESCRIPTION;
import static org.icgc.dcc.submission.core.report.ErrorParameterKey.EXPECTED;
import static org.icgc.dcc.submission.core.report.ErrorType.SCRIPT_ERROR;
import static org.icgc.dcc.submission.dictionary.model.RestrictionType.SCRIPT;
import lombok.val;

import org.icgc.dcc.submission.core.report.FieldErrorReport;
import org.icgc.dcc.submission.core.report.FileReport;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.dictionary.model.Restriction;
import org.icgc.dcc.submission.repository.DictionaryRepository;
import org.icgc.dcc.submission.repository.ReleaseRepository;
import org.icgc.dcc.submission.validation.primary.restriction.ScriptRestriction;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.mongodb.BasicDBObject;

/**
 * Temporary: see DCC-2085.
 */
public class MongoMaxSizeHack {

  static Optional<FileReport> augmentScriptErrors(Optional<FileReport> optional,
      ReleaseRepository releaseRepository, DictionaryRepository dictionaryRepository) {

    if (optional.isPresent()) {
      val fileReport = optional.get();
      val errorReports = fileReport.getErrorReports();

      val dictionary = dictionaryRepository.findDictionaryByVersion(
          releaseRepository.findNextReleaseDictionaryVersion());

      val fileType = fileReport.getFileType();
      val fileSchema = dictionary.getFileSchema(fileType);

      for (val errorReport : errorReports) {
        if (errorReport.getErrorType() == SCRIPT_ERROR) {
          for (val fieldReport : errorReport.getFieldErrorReports()) {
            val scriptConfig = getScript(fileSchema, getScriptRestrictionFieldName(fieldReport));

            fieldReport.addParameter(EXPECTED, scriptConfig.get(ScriptRestriction.PARAM).toString());
            fieldReport.addParameter(DESCRIPTION, scriptConfig.get(ScriptRestriction.PARAM_DESCRIPTION));
          }
        }
      }
    }
    return optional;
  }

  private static BasicDBObject getScript(FileSchema fileSchema, String fieldName) {
    val field = fileSchema.getField(fieldName);
    val scriptRestrictionOrdinal = 0; // FIXME: https://jira.oicr.on.ca/browse/DCC-2087
    val scriptRestrictions = newArrayList(filter(
        field.getRestrictions(),
        new Predicate<Restriction>() {

          @Override
          public boolean apply(Restriction restriction) {
            return restriction.getType() == SCRIPT;
          }

        }));
    checkState(scriptRestrictions.size() > scriptRestrictionOrdinal);
    val scriptRestriction = scriptRestrictions.get(scriptRestrictionOrdinal);
    val restrictionConfig = scriptRestriction.getConfig();
    return restrictionConfig;
  }

  private static String getScriptRestrictionFieldName(FieldErrorReport fieldReport) {
    val fieldNames = fieldReport.getFieldNames();
    checkState(fieldNames.size() == 1, "Expecting only one field name for script restriction (by design)");
    return fieldNames.get(0);
  }

}
