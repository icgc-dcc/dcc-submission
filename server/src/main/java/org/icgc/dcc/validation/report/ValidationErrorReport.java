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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.icgc.dcc.validation.ValidationErrorCode;
import org.icgc.dcc.validation.cascading.TupleState;
import org.icgc.dcc.validation.cascading.TupleState.TupleError;

import com.mongodb.BasicDBList;

/**
 * 
 */
public class ValidationErrorReport {

  private ValidationErrorCode errorType;

  private String description;

  private BasicDBList columns;

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
      this.addColumn(error.getParameters());
    }
  }

  /**
   * @param error
   */
  public ValidationErrorReport(TupleError error) {
    this.columns = new BasicDBList();
    this.setErrorType(error.getCode());
    this.setDescription(error.getMessage());
    this.addColumn(error.getParameters());
  }

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
  public BasicDBList getColumns() {
    return columns;
  }

  /**
   * @param columns the columns to set
   */
  public void addColumn(Map<String, Object> column) {
    column.put("count", 1);
    List<Integer> lines = new ArrayList<Integer>();
    lines.add((Integer) column.get("line"));
    column.put("lines", lines);
    column.remove("line");

    List<Object> values = new ArrayList<Object>();
    values.add(column.get("value"));
    column.put("values", values);
    column.remove("value");

    this.columns.add(column);
  }

  /**
   * @param column
   * @param error
   * @param l
   */
  public void updateColumn(Map<String, Object> column, TupleError error, int line) {
    column.put("count", Integer.valueOf((((Integer) column.get("count")) + 1)));

    List<Integer> lines = (List<Integer>) column.get("lines");
    lines.add(line);
    column.put("lines", lines);

    List<Object> values = (List<Object>) column.get("values");
    values.add(error.getParameters().get("value"));
    column.put("values", values);
  }
}
