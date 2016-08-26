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
package org.icgc.dcc.submission.web.advice;

import org.icgc.dcc.submission.release.ReleaseException;
import org.icgc.dcc.submission.web.DuplicateNameException;
import org.icgc.dcc.submission.web.InvalidNameException;
import org.icgc.dcc.submission.web.ServerErrorCode;
import org.icgc.dcc.submission.web.ServerErrorResponseMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.val;

@ControllerAdvice
public class ExceptionHandlerAdvice {

  @ExceptionHandler
  @ResponseBody
  ResponseEntity<ServerErrorResponseMessage> handle(Exception e) throws Exception {
    if (e instanceof DuplicateNameException) {
      return response(HttpStatus.BAD_REQUEST, ServerErrorCode.ALREADY_EXISTS, e);
    } else if (e instanceof InvalidNameException) {
      return response(HttpStatus.BAD_REQUEST, ServerErrorCode.INVALID_NAME, e);
    } else if (e instanceof ReleaseException) {
      return response(HttpStatus.BAD_REQUEST, ServerErrorCode.RELEASE_EXCEPTION, e);
    }

    throw e;
  }

  public ResponseEntity<ServerErrorResponseMessage> response(HttpStatus status, ServerErrorCode error, Exception e) {
    val body = new ServerErrorResponseMessage(error, e.getMessage());
    return ResponseEntity.status(status).body(body);
  }

}