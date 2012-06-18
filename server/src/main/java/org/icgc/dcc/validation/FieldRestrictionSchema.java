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

import java.util.List;

import com.google.common.collect.ImmutableList;

public class FieldRestrictionSchema {

  public enum Type {
    NUMBER, TEXT, FIELD_REFERENCE
  }

  public static class FieldRestrictionParameter {

    private final String key;

    private final Type type;

    private final String description;

    private final boolean repeated;

    public FieldRestrictionParameter(String key, Type type) {
      this(key, type, null, false);
    }

    public FieldRestrictionParameter(String key, Type type, String description) {
      this(key, type, description, false);
    }

    public FieldRestrictionParameter(String key, Type type, String description, boolean repeated) {
      this.key = key;
      this.type = type;
      this.description = description;
      this.repeated = repeated;
    }

    public String getKey() {
      return key;
    }

    public Type getType() {
      return type;
    }

    public boolean isRepeated() {
      return repeated;
    }

    public String getDescription() {
      return description;
    }

  }

  private final List<FieldRestrictionParameter> parameters;

  public FieldRestrictionSchema(FieldRestrictionParameter... parameters) {
    this.parameters = ImmutableList.copyOf(parameters);
  }

  public List<FieldRestrictionParameter> getParameters() {
    return parameters;
  }

}
