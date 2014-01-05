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
package org.icgc.dcc.submission.validation.key.error;

import lombok.Value;
import lombok.experimental.Builder;

import org.codehaus.jackson.annotate.JsonProperty;
import org.icgc.dcc.submission.validation.core.ErrorType;

@Value
@Builder(builderMethodName = "kvError")
public class KVError {

  @JsonProperty
  String fileName;
  @JsonProperty
  long lineNumber;
  @JsonProperty
  Object value;
  @JsonProperty
  ErrorType type;
  @JsonProperty
  Object[] params;

  private KVError(
      @JsonProperty("fileName") String fileName,
      @JsonProperty("lineNumber") long lineNumber,
      @JsonProperty("value") Object value,
      @JsonProperty("type") ErrorType type,
      @JsonProperty("params") Object[] params)
  {
    this.fileName = fileName;
    this.lineNumber = lineNumber;
    this.value = value;
    this.type = type;
    this.params = params;
  }

}
