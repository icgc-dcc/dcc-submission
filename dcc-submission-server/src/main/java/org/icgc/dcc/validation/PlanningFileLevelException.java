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
package org.icgc.dcc.validation;

import static com.google.common.base.Preconditions.checkArgument;

import org.icgc.dcc.validation.cascading.TupleState;

public class PlanningFileLevelException extends RuntimeException {

  // conventions: because we neither have a row nor a column to report this on to; TODO: revisit (not elegant)
  private static final String FILE_LEVEL_ERROR_COLUMN_NAME = "FileLevelError";

  private static final String FILE_LEVEL_ERROR_VALUE = null;

  private final String filename;

  private final TupleState tupleState;

  public PlanningFileLevelException(String filename, ValidationErrorCode errorCode, Object... params) {
    checkArgument(filename != null);
    checkArgument(errorCode != null);
    checkArgument(params != null);
    this.filename = filename;
    tupleState = new TupleState();
    tupleState.reportError(errorCode, FILE_LEVEL_ERROR_COLUMN_NAME, FILE_LEVEL_ERROR_VALUE, params);
  }

  public String getFilename() {
    return this.filename;
  }

  public TupleState getTupleState() {
    return this.tupleState;
  }
}
