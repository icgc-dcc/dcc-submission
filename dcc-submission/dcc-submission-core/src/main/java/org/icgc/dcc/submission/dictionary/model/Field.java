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
package org.icgc.dcc.submission.dictionary.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.hibernate.validator.constraints.NotBlank;
import org.icgc.dcc.submission.dictionary.visitor.DictionaryElement;
import org.icgc.dcc.submission.dictionary.visitor.DictionaryVisitor;

import com.google.code.morphia.annotations.Embedded;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

/**
 * Describes a field that has {@code Restriction}s and that is part of a {@code FileSchema}
 */
@Embedded
public class Field implements DictionaryElement, Serializable {

  @NotBlank
  private String name;

  private String label;

  private ValueType valueType;

  private SummaryType summaryType;

  @Valid
  private List<Restriction> restrictions;

  private boolean controlled;

  public Field() {
    super();
    this.restrictions = new ArrayList<Restriction>();
  }

  public Field(Field field) {
    super();
    this.name = field.getName();
    this.label = field.getLabel();
    this.valueType = field.getValueType();
    this.summaryType = field.getSummaryType();
    this.restrictions = field.getRestrictions();
  }

  @Override
  public void accept(DictionaryVisitor dictionaryVisitor) {
    dictionaryVisitor.visit(this);

    for (Restriction restriction : restrictions) {
      restriction.accept(dictionaryVisitor);
    }
  }

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

  public ValueType getValueType() {
    return valueType;
  }

  public void setValueType(ValueType valueType) {
    this.valueType = valueType;
  }

  public List<Restriction> getRestrictions() {
    return restrictions;
  }

  public void setRestrictions(List<Restriction> restrictions) {
    this.restrictions = restrictions;
  }

  public void addRestriction(Restriction restriction) {
    if (this.getRestriction(restriction.getType()).isPresent()) {
      throw new DuplicateRestrictionFoundException("Duplicate Restriction found with type: " + restriction.getType());
    }
    this.restrictions.add(restriction);
  }

  public Optional<Restriction> getRestriction(final String type) {
    return Iterables.tryFind(this.restrictions, new Predicate<Restriction>() {

      @Override
      public boolean apply(Restriction input) {
        return input.getType().equals(type);
      }
    });
  }

  public boolean removeRestriction(Restriction restriction) {
    return this.restrictions.remove(restriction);
  }

  public SummaryType getSummaryType() {
    return summaryType;
  }

  public void setSummaryType(SummaryType summaryType) {
    this.summaryType = summaryType;
  }

  public boolean isControlled() {
    return controlled;
  }

  public void setControlled(boolean controlled) {
    this.controlled = controlled;
  }

  public boolean hasCodeListRestriction() {
    return hasRestriction("codelist");
  }

  public boolean hasInRestriction() {
    return hasRestriction("in");
  }

  public boolean hasRequiredRestriction() {
    return hasRestriction("required");
  }

  public boolean hasRegexRestriction() {
    return hasRestriction("regex");
  }

  private boolean hasRestriction(String restrictionName) {
    for (Restriction restriction : restrictions) {
      if (restrictionName.equalsIgnoreCase(restriction.getType())) {
        return true;
      }
    }
    return false;
  }

}
