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

import java.util.List;

import org.hibernate.validator.constraints.NotBlank;

import com.google.code.morphia.annotations.Embedded;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

@Embedded
public class QueuedProject { // TODO: rename as Queued is misleading (also applies to Validating)

  @NotBlank
  private String key;

  private List<String> emails;

  public QueuedProject() {
    super();
  }

  public QueuedProject(String key, List<String> emails) {
    super();
    this.key = key;
    this.emails = emails;
  }

  public ImmutableList<String> getEmails() {
    return ImmutableList.copyOf(this.emails);
  }

  public String getKey() {
    return this.key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public void setEmails(List<String> emails) {
    this.emails = emails;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(QueuedProject.class) //
        .add("key", this.key) //
        .add("emails", this.emails) //
        .toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((emails == null) ? 0 : emails.hashCode());
    result = prime * result + ((key == null) ? 0 : key.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    if(obj == this) {
      return true;
    }
    if(getClass() != obj.getClass()) {
      return false;
    }
    final QueuedProject other = (QueuedProject) obj;
    return Objects.equal(this.key, other.key) //
        && Objects.equal(this.emails, other.emails);
  }

}
