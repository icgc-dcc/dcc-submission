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
package org.icgc.dcc.submission.release.model;

import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.val;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.icgc.dcc.core.model.DataType;
import org.icgc.dcc.core.model.DataType.DataTypes;
import org.icgc.dcc.core.model.Identifiable;
import org.mongodb.morphia.annotations.Embedded;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableList;

@Embedded
@Setter
@Getter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
// TODO: rename as Queued is misleading (also applies to Validating)
public class QueuedProject implements Identifiable {

  @NotBlank
  private String key;
  @NotEmpty
  private List<String> emails;
  @NotNull
  private List<String> dataTypes;

  @JsonIgnore
  @Override
  public String getId() {
    return key;
  }

  public QueuedProject() {
    this(null, Collections.<String> emptyList(), Collections.<String> emptyList());
  }

  public QueuedProject(String key, List<String> emails) {
    this(key, emails, Collections.<String> emptyList());
  }

  public List<String> getEmails() {
    return ImmutableList.copyOf(emails);
  }

  public List<DataType> getDataTypes() {
    val builder = ImmutableList.<DataType> builder();
    for (val name : dataTypes) {
      builder.add(DataTypes.valueOf(name));
    }

    return builder.build();
  }

}
