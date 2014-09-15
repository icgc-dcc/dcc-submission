/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.validation.key.surjectivity;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.key.core.KVFileType;
import org.icgc.dcc.submission.validation.key.data.KVEncounteredForeignKeys;
import org.icgc.dcc.submission.validation.key.data.KVPrimaryKeys;
import org.icgc.dcc.submission.validation.key.report.KVReporter;

/**
 * Validates surjective relations.
 */
@RequiredArgsConstructor
@Slf4j
public class SurjectivityValidator {

  public static final long SURJECTION_ERROR_LINE_NUMBER = -1;

  public void validateSurjection(
      KVFileType fileType,
      KVPrimaryKeys expectedKeys,
      KVEncounteredForeignKeys encounteredKeys,
      KVReporter reporter,
      KVFileType offendedFileType) {
    val valid = validateSurjectionErrors(
        expectedKeys,
        encounteredKeys,
        reporter,
        offendedFileType);
    log.info((valid ? "No" : "Some") + " surjection error found for file type '{}'", fileType);
  }

  private boolean validateSurjectionErrors(
      KVPrimaryKeys expectedKeys,
      KVEncounteredForeignKeys encounteredKeys,
      KVReporter reporter,
      KVFileType offendedFileType) {
    log.info("Validating potential surjectivity errors");

    boolean validFileType = true;
    for (val fileName : expectedKeys.getFilePaths()) {
      val expectedIterator = expectedKeys.getPrimaryKeys(fileName);
      while (expectedIterator.hasNext()) {
        val expected = expectedIterator.next();
        val validKey = encounteredKeys.encountered(expected);
        if (!validKey) {
          reporter.reportSurjectionError(offendedFileType, fileName, expected);
          validFileType = false;
        }
      }
    }
    return validFileType;
  }
}
