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

import java.util.Map;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import com.google.common.base.Objects;

/**
 * Represents a user session.
 */
public class UserSession {

  private final String userName;

  private final long creationTime;

  private final long lastWriteTime;

  private final Map<String, String> ioSessionMap;

  @JsonCreator
  public UserSession(
      @JsonProperty("userName")
      String userName,
      @JsonProperty("creationTime")
      long creationTime,
      @JsonProperty("lastWriteTime")
      long lastWriteTime,
      @JsonProperty("ioSessionMap")
      Map<String, String> ioSessionMap) {
    super();
    this.userName = userName;
    this.creationTime = creationTime;
    this.lastWriteTime = lastWriteTime;
    this.ioSessionMap = ioSessionMap;

  }

  public String getUserName() {
    return userName;
  }

  public Map<String, String> getIoSessionMap() {
    return ioSessionMap;
  }

  public long getCreationTime() {
    return creationTime;
  }

  public long getLastWriteTime() {
    return lastWriteTime;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(UserSession.class)
        .add("userName", this.userName)
        .add("creationTime", this.creationTime)
        .add("lastWriteTime", this.lastWriteTime)
        .add("ioSessionMap", this.ioSessionMap)
        .toString();
  }

}
