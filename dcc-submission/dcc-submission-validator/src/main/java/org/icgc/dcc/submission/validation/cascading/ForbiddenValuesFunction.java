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
package org.icgc.dcc.submission.validation.cascading;

import static cascading.tuple.Fields.ARGS;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static org.icgc.dcc.submission.validation.ValidationErrorCode.FORBIDDEN_VALUE_ERROR;

import java.util.List;

import org.icgc.dcc.submission.dictionary.model.Restriction;
import org.icgc.dcc.submission.validation.restriction.RequiredRestriction;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.tuple.TupleEntry;

/**
 * Systematically applied on every required field for now.
 * <p>
 * TODO: DCC-1076 - Would be better done from within {@link RequiredRestriction}.
 */
@SuppressWarnings("rawtypes")
public class ForbiddenValuesFunction extends BaseOperation implements Function {

  /**
   * Only used internally, this isn't configurable in the dictionary (unlike {@link Restriction} names).
   */
  public static final String NAME = "forbidden";

  /**
   * Former reserved values that must not appear in required data anymore.
   */
  public static final List<String> DEPRECATED_VALUES = newArrayList("-999");

  /**
   * Fields having a required restriction set on them (whether scrict or not).
   */
  private final List<String> requiredFieldnames;

  public ForbiddenValuesFunction(List<String> requiredFieldnames) {
    super(ARGS);
    this.requiredFieldnames = checkNotNull(requiredFieldnames);
  }

  @Override
  public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
    TupleEntry entry = functionCall.getArguments();
    TupleState state = ValidationFields.state(entry);

    for(Comparable fieldName : entry.getFields()) {
      // Only check for required fields (irrespective of whether it is a strict or non-strict requirement
      if(requiredFieldnames.contains(fieldName.toString())) {
        String value = entry.getString(fieldName);
        if(DEPRECATED_VALUES.contains(value)) {
          state.reportError(FORBIDDEN_VALUE_ERROR, fieldName.toString(), value, value);
        }
      }
    }
    functionCall.getOutputCollector().add(entry.getTupleCopy());
  }

}
