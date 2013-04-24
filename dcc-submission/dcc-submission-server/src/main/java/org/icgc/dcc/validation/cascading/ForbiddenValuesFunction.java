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
package org.icgc.dcc.validation.cascading;

import java.util.List;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.tuple.TupleEntry;
import static cascading.tuple.Fields.ARGS;
import static com.google.common.collect.Lists.newArrayList;
import static org.icgc.dcc.validation.ValidationErrorCode.FORBIDDEN_VALUE_ERROR;

/**
 * Systematically applied on every field for now, we may want to allow some fields to use those values on an individual
 * basis in the future.
 */
@SuppressWarnings("rawtypes")
public class ForbiddenValuesFunction extends BaseOperation implements Function {

  /**
   * Only used internally, this isn't configurable in the dictionary (unlike {@link Restriction} names).
   */
  public static final String NAME = "forbidden";

  /**
   * Former reserved values that must not appear in the data anymore.
   */
  private static final List<String> DEPRECATED_VALUES = newArrayList("-999");

  public ForbiddenValuesFunction() {
    super(ARGS);
  }

  @Override
  public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
    TupleEntry entry = functionCall.getArguments();
    TupleState state = ValidationFields.state(entry);

    for(Comparable fieldName : entry.getFields()) {
      String value = entry.getString(fieldName);
      if(DEPRECATED_VALUES.contains(value)) {
        state.reportError(FORBIDDEN_VALUE_ERROR, fieldName.toString(), value, value);
      }
    }
    functionCall.getOutputCollector().add(entry.getTupleCopy());
  }

}
