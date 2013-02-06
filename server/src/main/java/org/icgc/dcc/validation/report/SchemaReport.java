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
package org.icgc.dcc.validation.report;

import java.io.Serializable;
import java.util.List;

import com.google.code.morphia.annotations.Embedded;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

@Embedded
public class SchemaReport implements Serializable {

  protected String name;

  protected List<FieldReport> fieldReports;

  protected List<ValidationErrorReport> errors;

  public SchemaReport() {
    this.fieldReports = Lists.newArrayList();
    this.errors = Lists.newArrayList();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<FieldReport> getFieldReports() {
    return ImmutableList.copyOf(this.fieldReports);
  }

  public void setFieldReports(List<FieldReport> fieldReports) {
    this.fieldReports = fieldReports;
  }

  public List<ValidationErrorReport> getErrors() {
    return ImmutableList.copyOf(this.errors);
  }

  public void setErrors(List<ValidationErrorReport> errors) {
    this.errors = errors;
  }

  public void addError(ValidationErrorReport error) {
    this.errors.add(error);
  }

  public void addErrors(List<ValidationErrorReport> errors) {
    this.errors.addAll(errors);
  }

  public Optional<FieldReport> getFieldReport(final String field) {
    return Iterables.tryFind(fieldReports, new Predicate<FieldReport>() {

      @Override
      public boolean apply(FieldReport input) {
        return input.getName().equals(field);
      }
    });
  }

  public void addFieldReport(FieldReport fieldReport) {
    this.fieldReports.add(fieldReport);
  }

  public void addFieldReports(List<FieldReport> fieldReports) {
    this.fieldReports.addAll(fieldReports);
  }
}
