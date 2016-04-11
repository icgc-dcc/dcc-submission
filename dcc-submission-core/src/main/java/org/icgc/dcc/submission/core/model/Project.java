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
package org.icgc.dcc.submission.core.model;

import static org.icgc.dcc.submission.core.util.NameValidator.PROJECT_ID_PATTERN;

import java.util.Set;

import javax.validation.constraints.Pattern;

import org.bson.types.ObjectId;
import org.hibernate.validator.constraints.NotBlank;
import org.icgc.dcc.submission.core.model.Views.Digest;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity(noClassnameStored = true)
@Data
@NoArgsConstructor
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class Project {

  @Id
  @JsonIgnore
  private ObjectId id;

  @JsonView(Digest.class)
  @NotBlank
  @Pattern(regexp = PROJECT_ID_PATTERN)
  @Indexed(unique = true)
  private String key;

  @JsonView(Digest.class)
  @NotBlank
  private String name;

  @JsonView(Digest.class)
  private String alias;

  @NonNull
  private Set<String> users = Sets.newHashSet();

  @NonNull
  private Set<String> groups = Sets.newHashSet();

  public Project(String key, String name) {
    this.key = key;
    this.name = name;
  }

  public Set<String> getUsers() {
    return ImmutableSet.copyOf(users);
  }

  public Set<String> getGroups() {
    return ImmutableSet.copyOf(groups);
  }

  public boolean hasUser(String user) {
    return users.contains(user);
  }

}
