/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.dictionary.model;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.hibernate.validator.constraints.NotBlank;
import org.icgc.dcc.common.core.collect.SerializableMaps;
import org.icgc.dcc.submission.core.model.BaseEntity;
import org.icgc.dcc.submission.core.model.HasName;
import org.mongodb.morphia.annotations.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Function;

import lombok.ToString;

/**
 * Describes a list of codes (see {@code Term})
 */
@Entity
@ToString
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

  // TODO: DCC-904 - validation: ensure no value is a code (find reference to ticket in CodeListRestriction and
  // SubmissionFileSchemeHelper to find out why)
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

  @JsonIgnore
  public Map<String, String> asMap() {
    return SerializableMaps.<Term, String, String> transformListToMap(
        terms,
        new Function<Term, String>() {

          @Override
          public String apply(Term term) {
            return term.getCode();
          }

        },
        new Function<Term, String>() {

          @Override
          public String apply(Term term) {
            return term.getValue();
          }

        });
  }

}
