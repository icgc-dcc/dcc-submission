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
package org.icgc.dcc.submission.validation.service;

import java.util.List;

import lombok.NonNull;
import lombok.Value;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * A {@code Validation} is a high level container which encapsulates the execution context of sequential
 * {@link Validator}s.
 */
@Value
@Slf4j
public class Validation {

  /**
   * The per-instance context of the validation.
   */
  @NonNull
  private final ValidationContext context;

  /**
   * Sequence of validators to apply to the {@link #context}.
   */
  @NonNull
  private final List<Validator> validators;

  /**
   * The identifier used to {@code submit} and {@code cancel} with the {@link ValidationExecutor}.
   */
  public String getId() {
    return context.getProjectKey();
  }

  /**
   * Executes the sequence of {@link #validators}
   * 
   * @throws InterruptedException when interrupted by the {@link ValidationExecutor}
   */
  public void execute() throws InterruptedException {
    log.info("Executing validition '{}'...", getId());
    verifyState();

    for (val validator : validators) {
      log.info("Executing {} validator for '{}'...", validator.getClass().getSimpleName(), getId());
      validator.validate(context);

      if (context.hasErrors()) {
        log.info("Executing {} validator for '{}' has errors", validator.getClass().getSimpleName(), getId());
        break;
      }

      verifyState();
    }
  }

  /**
   * Checks if the validation has been canceled.
   * 
   * @throws InterruptedException when interrupted by the {@link ValidationExecutor}
   */
  private void verifyState() throws InterruptedException {
    val cancelled = Thread.currentThread().isInterrupted();
    if (cancelled) {
      throw new InterruptedException("Validation '" + getId() + "' was interrupted");
    }
  }

}
