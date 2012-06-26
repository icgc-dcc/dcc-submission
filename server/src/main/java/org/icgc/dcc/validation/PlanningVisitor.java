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

import java.util.List;

import org.icgc.dcc.model.dictionary.Field;
import org.icgc.dcc.model.dictionary.FileSchema;
import org.icgc.dcc.model.dictionary.visitor.BaseDictionaryVisitor;
import org.icgc.dcc.validation.plan.PlanElement;
import org.icgc.dcc.validation.plan.PlanPhase;

import com.google.common.collect.Lists;

public class PlanningVisitor extends BaseDictionaryVisitor {

  private final PlanPhase phase;

  private final List<PlanElement> elements = Lists.newArrayList();

  private FileSchema currentSchema;

  private Field currentField;

  public PlanningVisitor(PlanPhase phase) {
    this.phase = phase;
  }

  public PlanPhase getPhase() {
    return phase;
  }

  public List<PlanElement> getElements() {
    return elements;
  }

  public Field getCurrentField() {
    return currentField;
  }

  public FileSchema getCurrentSchema() {
    return currentSchema;
  }

  @Override
  public void visit(FileSchema fileSchema) {
    this.currentSchema = fileSchema;
    elements.clear();
  }

  @Override
  public void visit(Field field) {
    this.currentField = field;
  }

  protected void collect(PlanElement element) {
    this.elements.add(element);
  }

}
