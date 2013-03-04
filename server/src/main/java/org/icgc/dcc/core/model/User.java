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

import org.icgc.dcc.shiro.DccWrappingRealm;

import com.google.code.morphia.annotations.Entity;

/**
 * This class/collection is only intended to persist the number of failed login attempts per user. It should not try to
 * keep track of roles/permissions as it is the responsibility of <code>{@link DccWrappingRealm}</code> to do so.
 * Keeping track of those permissions would require updates on <code>{@link Project}</code> to cascade down changes to
 * this collection (a complexity we do not care for).
 * <p>
 * It is a workaround until the user management is fully figured out (https://jira.oicr.on.ca/browse/DCC-815 for Crowd
 * integration)
 */
@Entity
public class User extends BaseEntity implements HasName {

  protected String username;

  private int failedAttempts = 0;

  private static final int MAX_ATTEMPTS = 3;

  @Override
  public String getName() {
    return getUsername();
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public int getFailedAttempts() {
    return failedAttempts;
  }

  public void setFailedAttempts(int failedAttempts) {
    this.failedAttempts = failedAttempts;
  }

  public void incrementAttempts() {
    failedAttempts++;
  }

  public boolean isLocked() {
    return failedAttempts >= MAX_ATTEMPTS;
  }

  public void resetAttempts() {
    failedAttempts = 0;
  }

  @Override
  public String toString() {
    return "User [username=" + username + ", failedAttempts=" + failedAttempts + "]";
  }
}
