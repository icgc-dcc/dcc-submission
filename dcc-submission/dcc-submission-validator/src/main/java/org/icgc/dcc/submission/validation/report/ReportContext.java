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
package org.icgc.dcc.submission.validation.report;

import org.icgc.dcc.submission.validation.cascading.TupleState.TupleError;
import org.icgc.dcc.submission.validation.core.ErrorType;

/**
 * "Encapsulated Context Object" class that insulates and decouples the validation logic from report collection and
 * storage. Implementations should manage memory so that excessive reporting doesn't produce unstable memory pressure.
 * This is a "role partition" in the first link below.
 * 
 * @see http://www.two-sdg.demon.co.uk/curbralan/papers/europlop/ContextEncapsulation.pdf
 * @see http://www.allankelly.net/static/patterns/encapsulatecontext.pdf
 */
public interface ReportContext {

  /**
   * Indicates that at least one error was reported.
   */
  boolean hasErrors();

  /**
   * Returns the total number of errors encountered so far.
   */
  int getErrorCount();

  void reportError(String fileName, TupleError tupleError);

  void reportError(String fileName, long lineNumber, String columnName, Object value, ErrorType type, Object... params);

  void reportError(String fileName, long lineNumber, Object value, ErrorType type, Object... params);

  void reportError(String fileName, Object value, ErrorType type, Object... params);

  void reportError(String fileName, ErrorType type, Object... params);

  void reportError(String fileName, ErrorType type);

}
