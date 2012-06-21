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
package org.icgc.dcc.model.dictionary;

import java.util.ArrayList;
import java.util.List;

import org.icgc.dcc.model.dictionary.visitor.DictionaryElement;
import org.icgc.dcc.model.dictionary.visitor.DictionaryVisitor;

import com.google.code.morphia.annotations.Entity;

/**
 * 
 */
@Entity
public class Relation implements DictionaryElement {

  private final List<String> fields;

  private String other;

  private String allowOrphan;

  private String joinType;

  private final List<String> otherFields;

  public Relation() {
    fields = new ArrayList<String>();
    otherFields = new ArrayList<String>();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.icgc.dcc.model.dictionary.visitor.DictionaryElement#accept(org.icgc.dcc.model.dictionary.visitor.DictionaryVisitor
   * )
   */
  @Override
  public void accept(DictionaryVisitor dictionaryVisitor) {
    dictionaryVisitor.visit(this);
  }

  /**
   * @return the fields
   */
  public List<String> getFields() {
    return fields;
  }

  /**
   * @return the other
   */
  public String getOther() {
    return other;
  }

  /**
   * @param other the other to set
   */
  public void setOther(String other) {
    this.other = other;
  }

  /**
   * @return the otherFields
   */
  public List<String> getOtherFields() {
    return otherFields;
  }

  /**
   * @return the allowOrphan
   */
  public String getAllowOrphan() {
    return allowOrphan;
  }

  /**
   * @param allowOrphan the allowOrphan to set
   */
  public void setAllowOrphan(String allowOrphan) {
    this.allowOrphan = allowOrphan;
  }

  /**
   * @return the joinType
   */
  public String getJoinType() {
    return joinType;
  }

  /**
   * @param joinType the joinType to set
   */
  public void setJoinType(String joinType) {
    this.joinType = joinType;
  }

}
