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
package org.icgc.dcc.submission.validation.primary.visitor;

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.icgc.dcc.submission.dictionary.model.Field;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.dictionary.visitor.BaseDictionaryVisitor;
import org.icgc.dcc.submission.validation.primary.core.FlowType;
import org.icgc.dcc.submission.validation.primary.core.Plan;
import org.icgc.dcc.submission.validation.primary.core.PlanElement;

/**
 * A {@code DictionaryVisitor} that collects {@code PlanElement} during its visit. Elements are cleared upon each visit
 * of a new {@code FileSchema}.
 * 
 * @param <T> the type of {@code PlanElement} collected by this visitor
 */
@RequiredArgsConstructor
@Getter
public abstract class PlanningVisitor<T extends PlanElement> extends BaseDictionaryVisitor {

  @NonNull
  private final FlowType flowType;
  private final List<T> collectedPlanElements = newArrayList();

  /**
   * Transient state.
   */
  private FileSchema currentSchema;
  private Field currentField;

  /**
   * Applies the collected {@code PlanElement} to the specified {@code Plan}
   * @param plan
   */
  public abstract void apply(Plan plan);

  @Override
  public void visit(FileSchema fileSchema) {
    // Clear the collected elements. This allows re-using this instance for multiple plans
    collectedPlanElements.clear();
    currentSchema = fileSchema;
  }

  @Override
  public void visit(Field field) {
    currentField = field;
  }

  protected void collectPlanElement(T planElement) {
    collectedPlanElements.add(planElement);
  }

  protected List<T> getCollectedPlanElements() {
    return collectedPlanElements;
  }

}
