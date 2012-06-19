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
package org.icgc.dcc.validation;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.regex.Pattern;

import org.icgc.dcc.filesystem.SubmissionDirectory;
import org.icgc.dcc.model.dictionary.FileSchema;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Filter;
import cascading.operation.FilterCall;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.tap.Tap;

public class ValidationBundle {

  private final SubmissionDirectory directory;

  private final FileSchema schema;

  private Tap source;

  private Pipe tail;

  private Tap errors;

  ValidationBundle(SubmissionDirectory directory, FileSchema schema) {
    checkArgument(directory != null);
    checkArgument(schema != null);
    this.directory = directory;
    this.schema = schema;
    directory.listFile(Pattern.compile(schema.getPattern()));
  }

  public void extend(PipeExtender extender) {
    this.tail = extender.extend(tail);
  }

  public void complete() {
    Pipe invalid = new Each(tail, new TupleStateFilter(false));
  }

  private static class TupleStateFilter extends BaseOperation implements Filter {

    private final boolean valid;

    public TupleStateFilter(boolean valid) {
      this.valid = valid;
    }

    @Override
    public boolean isRemove(FlowProcess flowProcess, FilterCall filterCall) {
      return ValidationFields.state(filterCall.getArguments()).isValid() != valid;
    }

  }

}
