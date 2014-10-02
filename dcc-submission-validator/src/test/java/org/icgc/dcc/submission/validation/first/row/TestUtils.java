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
package org.icgc.dcc.submission.validation.first.row;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.icgc.dcc.submission.core.report.Error;
import org.icgc.dcc.submission.validation.core.ValidationContext;

public class TestUtils {

  public static void checkErrorReported(ValidationContext validationContext, int times) {
    verify(validationContext, times(times)).reportError(any(Error.class));
  }

  public static void checkFileCollisionErrorReported(ValidationContext validationContext, int times) {
    verify(validationContext, times(times)).reportError(any(Error.class));
  }

  public static void checkRowCharsetErrorReported(ValidationContext validationContext, int times) {
    verify(validationContext, times(times)).reportError(any(Error.class));
  }

  public static void checkRowColumnErrorReported(ValidationContext validationContext, int times) {
    verify(validationContext, times(times)).reportError(any(Error.class));
  }

  public static void checkFileHeaderErrorReported(ValidationContext validationContext, int times) {
    verify(validationContext, times(times)).reportError(any(Error.class));
  }

  public static void checkReferentialErrorReported(ValidationContext validationContext, int times) {
    verify(validationContext, times(times)).reportError(any(Error.class));
  }

  public static void checkNoErrorsReported(ValidationContext validationContext) {
    verify(validationContext, never()).reportError(any(Error.class));
  }

}
