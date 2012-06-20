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
package org.icgc.dcc.validation.cascading;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Filter;
import cascading.operation.FilterCall;

public final class TupleStates {

  private static class TupleStateFilter extends BaseOperation implements Filter {

    static final TupleStateFilter validFilter = new TupleStateFilter(true);

    static final TupleStateFilter invalidFilter = new TupleStateFilter(false);

    private final boolean valid;

    private TupleStateFilter(boolean valid) {
      this.valid = valid;
    }

    @Override
    public boolean isRemove(FlowProcess flowProcess, FilterCall filterCall) {
      return ValidationFields.state(filterCall.getArguments()).isValid() != valid;
    }

  }

  /**
   * Returns a {@code Filter} that will only keep {@code valid} tuples based on its {@code TupleState}. Note that the
   * {@code ValidationFields#STATE_FIELD} must be one of the operation's arguments.
   * 
   * @return a {@code Filter} instance usable to keep valid tuples only
   */
  public static Filter keepValidTuplesFilter() {
    return TupleStateFilter.validFilter;
  }

  /**
   * Returns a {@code Filter} that will only keep {@code invalid} tuples based on its {@code TupleState}. Note that the
   * {@code ValidationFields#STATE_FIELD} must be one of the operation's arguments.
   * 
   * @return a {@code Filter} instance usable to keep {@code invalid} tuples only
   */
  public static Filter keepInvalidTuplesFilter() {
    return TupleStateFilter.invalidFilter;
  }

}
