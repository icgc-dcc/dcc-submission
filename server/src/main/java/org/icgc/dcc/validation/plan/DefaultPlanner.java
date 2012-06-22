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
package org.icgc.dcc.validation.plan;

import java.io.File;
import java.io.FileFilter;
import java.util.List;
import java.util.Map;

import org.icgc.dcc.model.dictionary.FileSchema;

import cascading.cascade.Cascade;
import cascading.cascade.CascadeConnector;
import cascading.cascade.CascadeDef;
import cascading.flow.FlowConnector;
import cascading.flow.FlowDef;
import cascading.scheme.local.TextDelimited;
import cascading.tap.Tap;
import cascading.tap.local.FileTap;
import cascading.tuple.Fields;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

public class DefaultPlanner implements Planner {

  private final File root;

  private final File output;

  private final Map<String, FileSchemaPlanner> plans = Maps.newHashMap();

  private final FlowConnector flowConnector;

  public DefaultPlanner(File root, File output, FlowConnector flowConnector) {
    this.root = root;
    this.output = output;
    this.flowConnector = flowConnector;
  }

  @Override
  public void prepare(FileSchema schema) {
    this.plans.put(schema.getName(), new DefaultFileSchemaPlanner(this, schema));
  }

  @Override
  public List<FileSchemaPlanner> getSchemaPlans() {
    return ImmutableList.copyOf(plans.values());
  }

  @Override
  public FileSchemaPlanner getSchemaPlan(String schema) {
    FileSchemaPlanner schemaPlan = this.plans.get(schema);
    if(schemaPlan == null) throw new IllegalStateException("no plan for " + schema);
    return schemaPlan;
  }

  @Override
  public Cascade plan() {
    CascadeDef def = new CascadeDef();
    for(FileSchemaPlanner plan : getSchemaPlans()) {
      def.addFlow(flowConnector.connect(plan.internalFlow()));
      FlowDef external = plan.externalFlow();
      if(external != null) {
        def.addFlow(flowConnector.connect(external));
      }
    }
    return new CascadeConnector().connect(def);
  }

  @Override
  public Tap getSourceTap(String schema) {
    return tap(file(root, getSchemaPlan(schema).getSchema()));
  }

  @Override
  public Tap getInternalSinkTap(String schema) {
    return tap(new File(output, schema + ".internal.tsv"));
  }

  @Override
  public Tap getExternalSinkTap(String schema) {
    return tap(new File(output, schema + ".external.tsv"));
  }

  @Override
  public Tap getTrimmedTap(String schema, String[] fields) {
    File trimmed = new File(output, schema + "-" + Joiner.on("_").join(fields) + ".tsv");
    return new FileTap(new TextDelimited(new Fields(fields), true, "\t"), trimmed.getAbsolutePath());
  }

  private Tap tap(File file) {
    return new FileTap(new TextDelimited(true, "\t"), file.getAbsolutePath());
  }

  private File file(File root, final FileSchema schema) {
    File[] files = root.listFiles(new FileFilter() {

      @Override
      public boolean accept(File pathname) {
        return pathname.getName().contains(schema.getName());
        // return Pattern.matches(fs.getPattern(), pathname.getName());
      }
    });
    return files[0];
  }
}
