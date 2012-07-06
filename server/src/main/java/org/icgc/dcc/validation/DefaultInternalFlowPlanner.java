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

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.validation.cascading.AddValidationFieldsFunction;
import org.icgc.dcc.validation.cascading.RemoveEmptyLineFilter;
import org.icgc.dcc.validation.cascading.RemoveHeaderFilter;
import org.icgc.dcc.validation.cascading.StructralCheckFunction;
import org.icgc.dcc.validation.cascading.TupleStates;
import org.icgc.dcc.validation.cascading.ValidationFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cascading.flow.Flow;
import cascading.flow.FlowDef;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.pipe.assembly.Retain;
import cascading.tap.Tap;
import cascading.tuple.Fields;

import com.google.common.collect.Maps;

class DefaultInternalFlowPlanner implements InternalFlowPlanner {

  private static final Logger log = LoggerFactory.getLogger(DefaultInternalFlowPlanner.class);

  private final Plan plan;

  private final FileSchema fileSchema;

  private final Pipe head;

  private final Map<Trim, Pipe> trimmedTails = Maps.newHashMap();

  private Pipe validTail;

  private StructralCheckFunction structralCheck;

  DefaultInternalFlowPlanner(Plan plan, FileSchema fileSchema) {
    checkArgument(plan != null);
    checkArgument(fileSchema != null);
    this.plan = plan;
    this.fileSchema = fileSchema;
    this.validTail = this.head = new Pipe(fileSchema.getName());

    // apply pipe for removing empty line
    this.validTail = applyEmptyLineFilterPipes(this.validTail);
    // apply pipe for removing header
    this.validTail = applyHeaderFilterPipes(this.validTail);
    // apply structural check for making sure # of fields is matched with header
    this.validTail = applyStructuralCheck(this.validTail);
    // apply system pipe
    this.validTail = applySystemPipes(this.validTail);
  }

  @Override
  public String getName() {
    return getSchema().getName() + ".internal";
  }

  @Override
  public FileSchema getSchema() {
    return fileSchema;
  }

  @Override
  public void apply(InternalPlanElement element) {
    checkArgument(element != null);
    log.info("[{}] applying element [{}]", getName(), element.describe());
    validTail = element.extend(validTail);
  }

  @Override
  public Trim addTrimmedOutput(String... fields) {
    checkArgument(fields != null);
    checkArgument(fields.length > 0);
    Trim trim = new Trim(fileSchema.getName(), fields);
    if(trimmedTails.containsKey(trim) == false) {
      Pipe newHead = new Pipe(trim.getName(), validTail);
      Pipe tail = new Retain(newHead, new Fields(fields));
      log.info("[{}] planned trimmed output with {}", getName(), Arrays.toString(trim.getFields()));
      trimmedTails.put(trim, tail);
    }
    return trim;
  }

  @Override
  public Flow<?> connect(CascadingStrategy strategy) {
    Pipe tail = applyFilter(validTail);
    Tap<?, ?, ?> source = strategy.getSourceTap(fileSchema);
    Tap<?, ?, ?> sink = strategy.getInternalSinkTap(fileSchema);
    try {
      Fields header = strategy.getFileHeader(fileSchema);
      this.structralCheck.setFieldDeclaration(header);
      this.structralCheck.setFileSchema(fileSchema);

    } catch(IOException e) {
      e.printStackTrace();
    }

    FlowDef def = new FlowDef()//
        .setName(getName())//
        .addSource(head, source)//
        .addTailSink(tail, sink);

    for(Map.Entry<Trim, Pipe> e : trimmedTails.entrySet()) {
      def.addTailSink(e.getValue(), strategy.getTrimmedTap(e.getKey()));
    }
    return strategy.getFlowConnector().connect(def);
  }

  private Pipe applyStructuralCheck(Pipe pipe) {
    this.structralCheck = new StructralCheckFunction();
    return new Each(pipe, this.structralCheck, Fields.RESULTS);
  }

  private Pipe applySystemPipes(Pipe pipe) {
    return new Each(pipe, new AddValidationFieldsFunction(), Fields.ALL);
  }

  private Pipe applyHeaderFilterPipes(Pipe pipe) {
    return new Each(pipe, new RemoveHeaderFilter());
  }

  private Pipe applyEmptyLineFilterPipes(Pipe pipe) {
    return new Each(pipe, new RemoveEmptyLineFilter());
  }

  private Pipe applyFilter(Pipe pipe) {
    return new Retain(new Each(pipe, TupleStates.keepInvalidTuplesFilter()), ValidationFields.STATE_FIELD);
  }

}
