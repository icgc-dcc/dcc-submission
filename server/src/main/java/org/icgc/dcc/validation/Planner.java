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

import java.util.regex.Pattern;

import org.icgc.dcc.filesystem.SubmissionDirectory;
import org.icgc.dcc.model.dictionary.Dictionary;
import org.icgc.dcc.model.dictionary.FileSchema;

import cascading.cascade.Cascade;
import cascading.cascade.CascadeConnector;
import cascading.cascade.CascadeDef;
import cascading.flow.FlowDef;
import cascading.pipe.Pipe;
import cascading.scheme.hadoop.TextLine;
import cascading.tap.hadoop.Hfs;

import com.google.common.collect.Iterables;

public class Planner {

  SubmissionDirectory directory;

  Dictionary dictionary;

  public Planner(SubmissionDirectory directory, Dictionary dictionary) {
    this.directory = directory;
    this.dictionary = dictionary;
  }

  public Cascade plan() {
    CascadeDef c = new CascadeDef();
    for(FileSchema fs : dictionary.getFiles()) {
      String file = Iterables.getFirst(directory.listFile(Pattern.compile(fs.getPattern())), null);
      if(file != null) {
        FlowDef flow = makeFlow(fs, file);
      }
    }
    return new CascadeConnector().connect(c);
  }

  private FlowDef makeFlow(FileSchema fs, String file) {
    FlowDef fd = new FlowDef();
    Pipe pipe;
    fd.addSource(pipe = new Pipe(fs.getName()), new Hfs(new TextLine(), file));
    fd.addSink(fs.getName() + "internal.tsv", new Hfs(new TextLine(), file));

    return fd;
  }
}
