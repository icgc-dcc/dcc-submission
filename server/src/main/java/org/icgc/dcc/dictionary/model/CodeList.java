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
package org.icgc.dcc.dictionary.model;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.hibernate.validator.constraints.NotBlank;
import org.icgc.dcc.core.model.BaseEntity;
import org.icgc.dcc.core.model.HasName;

import com.google.code.morphia.annotations.Entity;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Describes a list of codes (see {@code Term})
 */
@Entity
public class CodeList extends BaseEntity implements HasName {

  @NotBlank
  private String name;

  private String label;

  @Valid
  private List<Term> terms;

  public CodeList() {
    super();
    terms = new ArrayList<Term>();
  }

  public CodeList(String name) {
    this();
    this.name = name;
    this.label = name;
    checkArgument(label != null);
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public List<Term> getTerms() {
    return terms;
  }

  public void setTerms(List<Term> terms) {
    this.terms = terms;
  }

  public void addTerm(Term term) {
    terms.add(term);
  }

  public boolean containsTerm(Term term) {
    return terms.contains(term);
  }

  @Override
  public String toString() {
    return "CodeList [name=" + name + ", label=" + label + ", terms=" + terms + "]";
  }
}
