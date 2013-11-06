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
package org.icgc.dcc.submission.validation.core;

import static com.google.common.collect.Iterables.tryFind;
import static com.google.common.collect.Lists.newArrayList;

import java.io.Serializable;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import com.google.code.morphia.annotations.Embedded;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

@Embedded
@Setter
@ToString
public class SchemaReport implements Serializable {

  @Getter
  protected String name;
  protected List<FieldReport> fieldReports = newArrayList();
  protected List<ErrorReport> errors = newArrayList();

  public List<FieldReport> getFieldReports() {
    return ImmutableList.copyOf(fieldReports);
  }

  public List<ErrorReport> getErrors() {
    return ImmutableList.copyOf(errors);
  }

  public void addError(ErrorReport error) {
    errors.add(error);
  }

  public void addErrors(List<ErrorReport> errors) {
    this.errors.addAll(errors);
  }

  public Optional<FieldReport> getFieldReport(final String field) {
    return tryFind(fieldReports, new Predicate<FieldReport>() {

      @Override
      public boolean apply(FieldReport input) {
        return input.getName().equals(field);
      }

    });
  }

  public void addFieldReport(FieldReport fieldReport) {
    fieldReports.add(fieldReport);
  }

  public void addFieldReports(List<FieldReport> fieldReports) {
    this.fieldReports.addAll(fieldReports);
  }

}
