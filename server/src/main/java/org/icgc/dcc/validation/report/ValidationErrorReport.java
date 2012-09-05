/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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
package org.icgc.dcc.validation.report;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.icgc.dcc.validation.ValidationErrorCode;
import org.icgc.dcc.validation.cascading.TupleState;
import org.icgc.dcc.validation.cascading.TupleState.TupleError;

/**
 * 
 */
public class ValidationErrorReport {

  public ValidationErrorReport() {
  }

  /**
   * @param tupleState
   */
  public ValidationErrorReport(TupleState tupleState) {
    Iterator<TupleState.TupleError> errors = tupleState.getErrors().iterator();
    if(errors.hasNext()) {
      TupleState.TupleError error = errors.next();
      this.setErrorType(error.getCode());
      this.setDescription(error.getMessage());
    }
  }

  /**
   * @param error
   */
  public ValidationErrorReport(TupleError error) {
    this.setErrorType(error.getCode());
    this.setDescription(error.getMessage());

    // this.columns.add(error.getParameters()[0]);
  }

  public ValidationErrorCode errorType;

  public String description;

  public List<Map<String, ? extends Object>> columns;

  /**
   * @return the errorType
   */
  public ValidationErrorCode getErrorType() {
    return errorType;
  }

  /**
   * @param errorType the errorType to set
   */
  public void setErrorType(ValidationErrorCode errorType) {
    this.errorType = errorType;
  }

  /**
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * @param description the description to set
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * @return the columns
   */
  public List<Map<String, ? extends Object>> getColumns() {
    return columns;
  }

  /**
   * @param columns the columns to set
   */
  public void setColumns(List<Map<String, ? extends Object>> columns) {
    this.columns = columns;
  }
}
