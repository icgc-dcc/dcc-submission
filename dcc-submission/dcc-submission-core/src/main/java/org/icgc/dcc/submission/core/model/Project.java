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
package org.icgc.dcc.submission.core.model;

import static com.google.common.base.Objects.firstNonNull;
import static org.icgc.dcc.submission.core.util.NameValidator.PROJECT_ID_PATTERN;

import java.util.List;

import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.NotBlank;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Indexed;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

@Entity
public class Project extends BaseEntity implements HasName {

  @NotBlank
  @Pattern(regexp = PROJECT_ID_PATTERN)
  @Indexed(unique = true)
  protected String key;

  @NotBlank
  protected String name;

  protected String alias;

  protected List<String> users = Lists.newArrayList();

  protected List<String> groups = Lists.newArrayList();

  public Project() {
    super();
  }

  public Project(String name) {
    super();
    this.setName(name);
  }

  public Project(String name, String key) {
    super();
    this.setName(name);
    this.setKey(key);
  }

  @Override
  public String getName() {
    return firstNonNull(name, getKey());
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAlias() {
    return alias;
  }

  public void setAlias(String alias) {
    this.alias = alias;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public List<String> getUsers() {
    return users;
  }

  public void setUsers(List<String> users) {
    this.users = users;
  }

  public List<String> getGroups() {
    return groups == null ? ImmutableList.<String> of() : ImmutableList.copyOf(groups);
  }

  public void setGroups(List<String> groups) {
    this.groups = groups;
  }

  public boolean hasUser(String name) {
    return this.users != null && this.users.contains(name);
  }

  @Override
  public String toString() {
    return "Project [key=" + key + ", name=" + name + ", alias=" + alias + ", users=" + users + ", groups=" + groups
        + "]";
  }

}
