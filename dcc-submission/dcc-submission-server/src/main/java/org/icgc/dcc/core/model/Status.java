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
package org.icgc.dcc.core.model;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import com.google.common.base.Objects;
import com.google.common.util.concurrent.Service.State;

/**
 * Represents the status of user sessions.
 */
public class Status {

  private final boolean sftpEnabled;

  private final State sftpState;

  private int activeSftpSessions;

  private final List<UserSession> userSessions;

  @JsonCreator
  public Status(
      @JsonProperty("sftpEnabled")
      Boolean sftpEnabled,

      @JsonProperty("sftpState")
      State sftpState,

      @JsonProperty("activeSftpSessions")
      int activeSftpSessions,

      @JsonProperty("userSessions")
      List<UserSession> userSessions)

  {
    super();
    this.sftpEnabled = sftpEnabled;
    this.sftpState = sftpState;
    this.activeSftpSessions = activeSftpSessions;
    this.userSessions = userSessions;
  }

  public Status(boolean sftpEnabled, State sftpState) {
    this.sftpEnabled = sftpEnabled;
    this.sftpState = checkNotNull(sftpState);
    this.activeSftpSessions = 0;
    this.userSessions = newArrayList();
  }

  public void addUserSession(UserSession userSession) { 
    // TODO: builder
    userSessions.add(userSession);
    activeSftpSessions++;
  }

  public boolean isSftpEnabled() {
    return sftpEnabled;
  }

  public State getSftpState() {
    return sftpState;
  }

  public int getActiveSftpSessions() {
    return activeSftpSessions;
  }

  public List<UserSession> getUserSessions() {
    return userSessions;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(Status.class)
        .add("sftpEnabled", this.sftpEnabled)
        .add("sftpState", this.sftpState)
        .add("activeSftpSessions", this.activeSftpSessions)
        .add("userSessions", this.userSessions)
        .toString();
  }

}
