/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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
package org.icgc.dcc.validation;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Map;

import org.icgc.dcc.validation.cascading.TupleState.TupleError;

import com.google.common.collect.Maps;

/**
 * Singleton that holds all error codes
 */
public class ErrorCodeRegistry {

  private static final ErrorCodeRegistry instance = new ErrorCodeRegistry();

  private final Map<Integer, String> formats = Maps.newHashMap();

  public static ErrorCodeRegistry get() {
    return instance;
  }

  public void register(int code, String format) {
    checkArgument(format != null);
    if(this.formats.containsKey(code)) {
      throw new IllegalArgumentException("error code " + code + " already registered with value "
          + this.formats.get(code));
    }
    this.formats.put(code, format);
  }

  public String format(TupleError error) {
    checkArgument(error != null);
    if(formats.containsKey(error.code()) == false) {
      return error.toString();
    }
    return String.format(formats.get(error.code()), error.parameters());
  }
}
