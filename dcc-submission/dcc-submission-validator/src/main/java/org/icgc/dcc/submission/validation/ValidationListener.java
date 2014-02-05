/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.validation;

import org.icgc.dcc.submission.validation.core.Validation;

/**
 * Abstraction that handles {@link ValidationExecutor} events.
 */
public interface ValidationListener {

  void onStarted(Validation validation);

  void onCancelled(Validation validation);

  void onCompletion(Validation validation);

  void onFailure(Validation validation, Throwable throwable);

  /**
   * Default no-op listener that does nothing.
   */
  public static final ValidationListener NOOP_LISTENER = new ValidationListener() {

    @Override
    public void onStarted(Validation validation) {
      // No-op
    }

    @Override
    public void onCompletion(Validation validation) {
      // No-op
    }

    @Override
    public void onCancelled(Validation validation) {
      // No-op
    }

    @Override
    public void onFailure(Validation validation, Throwable throwable) {
      // No-op
    }

  };

}
