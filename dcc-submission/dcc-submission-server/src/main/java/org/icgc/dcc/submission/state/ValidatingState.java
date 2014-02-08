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
package org.icgc.dcc.submission.state;

import static lombok.AccessLevel.PACKAGE;
import static org.icgc.dcc.submission.validation.core.ValidationOutcome.CANCELLED;
import static org.icgc.dcc.submission.validation.core.ValidationOutcome.FAILED;
import static org.icgc.dcc.submission.validation.core.ValidationOutcome.SUCCEEDED;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.submission.validation.core.SubmissionReport;
import org.icgc.dcc.submission.validation.core.ValidationOutcome;

@NoArgsConstructor(access = PACKAGE)
public class ValidatingState extends AbstractState {

  @Override
  public void finishValidation(@NonNull StateContext context, @NonNull ValidationOutcome outcome,
      @NonNull SubmissionReport submissionReport) {
    if (outcome == SUCCEEDED) {
      val valid = true;
      if (valid) {
        context.setState(VALID);
      } else {
        context.setState(NOT_VALIDATED);
      }
    } else if (outcome == FAILED) {
      context.setState(ERROR);
    } else if (outcome == CANCELLED) {
      context.setState(NOT_VALIDATED);
    }
  }

}