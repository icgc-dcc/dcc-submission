/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.validation.key.core;

import static org.icgc.dcc.submission.core.report.ErrorType.RELATION_PARENT_VALUE_ERROR;
import static org.icgc.dcc.submission.core.report.ErrorType.RELATION_VALUE_ERROR;
import static org.icgc.dcc.submission.core.report.ErrorType.UNIQUE_VALUE_ERROR;
import static org.icgc.dcc.submission.validation.key.core.KVKeyType.CONDITIONAL_FK;
import static org.icgc.dcc.submission.validation.key.core.KVKeyType.FK;
import static org.icgc.dcc.submission.validation.key.core.KVKeyType.OPTIONAL_FK;
import static org.icgc.dcc.submission.validation.key.core.KVKeyType.PK;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.icgc.dcc.submission.core.report.ErrorType;

/**
 * Type of key validator errors.
 */
@RequiredArgsConstructor
public enum KVErrorType {
  UNIQUENESS(PK, UNIQUE_VALUE_ERROR),
  RELATION(FK, RELATION_VALUE_ERROR),
  OPTIONAL_RELATION(OPTIONAL_FK, RELATION_VALUE_ERROR), // TODO: we should distinguish with primary (for ErrorType)
  CONDITIONAL_RELATION(CONDITIONAL_FK, RELATION_VALUE_ERROR),
  SURJECTION(PK, RELATION_PARENT_VALUE_ERROR);

  /**
   * The fields on which the error is reported.
   */
  @Getter
  private final KVKeyType keysType;

  @Getter
  private final ErrorType errorType;

}
