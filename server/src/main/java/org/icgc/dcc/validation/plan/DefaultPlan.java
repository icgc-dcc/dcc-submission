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
import cascading.scheme.local.TextDelimited;
import cascading.tap.local.FileTap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

public class DefaultPlan implements Plan {

  private final Map<String, FileSchemaPlan> plans = Maps.newHashMap();

  public DefaultPlan() {

  }

  @Override
  public void prepare(FileSchema schema) {
    this.plans.put(schema.getName(), new DefaultFileSchemaPlan(this, schema));
  }

  @Override
  public List<FileSchemaPlan> getSchemaPlans() {
    return ImmutableList.copyOf(plans.values());
  }

  @Override
  public FileSchemaPlan getPlan(String schema) {
    FileSchemaPlan schemaPlan = this.plans.get(schema);
    if(schemaPlan == null) throw new IllegalStateException("no plan for " + schema);
    return schemaPlan;
  }

  @Override
  public Cascade plan(File root, File output) {
    CascadeDef def = new CascadeDef();
    for(FileSchemaPlan plan : getSchemaPlans()) {
      File in = file(root, plan.getSchema());
      File out = new File(output, plan.getSchema().getName() + ".tsv");

      def.addFlow(plan.connect(new FileTap(new TextDelimited(true, "\t"), in.getAbsolutePath()), new FileTap(
          new TextDelimited(true, "\t"), out.getAbsolutePath())));
    }
    return new CascadeConnector().connect(def);
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
