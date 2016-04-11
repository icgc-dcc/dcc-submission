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
package org.icgc.dcc.submission.validation.key.data;

import org.icgc.dcc.submission.validation.key.core.KVFileType;
import org.icgc.dcc.submission.validation.key.report.KVReporter;

import com.google.common.base.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Context object representing the surrounding context of a row under processing.
 */
@RequiredArgsConstructor
@Getter
public class KVRowContext {

  /**
   * Immutable
   */
  private final String fileName;
  private final KVFileType fileType;
  private final KVReporter reporter;
  private final KVPrimaryKeys primaryKeys;
  private final Optional<KVReferencedPrimaryKeys> optionallyReferencedPrimaryKeys1;
  private final Optional<KVReferencedPrimaryKeys> optionallyReferencedPrimaryKeys2;
  private final Optional<KVEncounteredForeignKeys> optionallyEncounteredKeys;

  /**
   * Transient
   */
  private KVRow row;
  private long lineNumber;

  /**
   * Update the context to reflect the new row.
   * 
   * @param row the next row under processing
   * @param lineNumber the line number of the row in its associated file
   */
  public void nextRow(KVRow row, long lineNumber) {
    this.row = row;
    this.lineNumber = lineNumber;
  }

}