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

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Filter;
import cascading.operation.FilterCall;

public final class TupleStates {

  @SuppressWarnings("rawtypes")
  private static class TupleStateFilter extends BaseOperation implements Filter {

    enum State {
      VALID, INVALID, STRUCTURALLY_VALID, STRUCTURALLY_INVALID
    };

    static final TupleStateFilter validFilter = new TupleStateFilter(State.VALID);

    static final TupleStateFilter invalidFilter = new TupleStateFilter(State.INVALID);

    static final TupleStateFilter structurallyValidFilter = new TupleStateFilter(State.STRUCTURALLY_VALID);

    static final TupleStateFilter structurallyInvalidFilter = new TupleStateFilter(State.STRUCTURALLY_INVALID);

    private final State state;

    private TupleStateFilter(State state) {
      this.state = state;
    }

    @Override
    public boolean isRemove(FlowProcess flowProcess, FilterCall filterCall) {
      TupleState tupleState = ValidationFields.state(filterCall.getArguments());
      switch(state) {
      case VALID:
        return tupleState.isInvalid();
      case INVALID:
        return tupleState.isValid();
      case STRUCTURALLY_VALID:
        return tupleState.isStructurallyValid() == false;
      case STRUCTURALLY_INVALID:
        return tupleState.isStructurallyValid();
      default:
        throw new IllegalStateException();
      }
    }

  }

  /**
   * Returns a {@code Filter} that will only keep {@code valid} tuples based on its {@code TupleState}. Note that the
   * {@code ValidationFields#STATE_FIELD} must be one of the operation's arguments.
   * 
   * @return a {@code Filter} instance usable to keep valid tuples only
   */
  @SuppressWarnings("rawtypes")
  public static Filter keepValidTuplesFilter() {
    return TupleStateFilter.validFilter;
  }

  /**
   * Returns a {@code Filter} that will only keep {@code invalid} tuples based on its {@code TupleState}. Note that the
   * {@code ValidationFields#STATE_FIELD} must be one of the operation's arguments.
   * 
   * @return a {@code Filter} instance usable to keep {@code invalid} tuples only
   */
  @SuppressWarnings("rawtypes")
  public static Filter keepInvalidTuplesFilter() {
    return TupleStateFilter.invalidFilter;
  }

  /**
   * Returns a {@code Filter} that will only keep {@code structurally valid} tuples based on its {@code TupleState} (if
   * it DOESN'T contain {@code ValidationErrorCode#STRUCTURALLY_INVALID_ROW_ERROR}). Note that the
   * {@code ValidationFields#STATE_FIELD} must be one of the operation's arguments.
   * 
   * @return a {@code Filter} instance usable to keep {@code structurally valid} tuples only
   */
  @SuppressWarnings("rawtypes")
  public static Filter keepStructurallyValidTuplesFilter() {
    return TupleStateFilter.structurallyValidFilter;
  }

  /**
   * Returns a {@code Filter} that will only keep {@code structurally invalid} tuples based on its {@code TupleState}
   * (if it DOES contain {@code ValidationErrorCode#STRUCTURALLY_INVALID_ROW_ERROR}). Note that the
   * {@code ValidationFields#STATE_FIELD} must be one of the operation's arguments.
   * 
   * @return a {@code Filter} instance usable to keep {@code structurally valid} tuples only
   */
  @SuppressWarnings("rawtypes")
  public static Filter keepStructurallyInvalidTuplesFilter() {
    return TupleStateFilter.structurallyInvalidFilter;
  }
}
