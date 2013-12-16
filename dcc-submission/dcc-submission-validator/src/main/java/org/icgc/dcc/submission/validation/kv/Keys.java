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
package org.icgc.dcc.submission.validation.kv;

import java.util.List;

import lombok.Value;
import lombok.val;

// TODO: efficient equals/hashCode (maybe lombok is ok for the latter)
@Value
public class Keys implements Comparable<Keys> {

  public static final Keys NOT_APPLICABLE = null;

  private final short size;
  private final String[] keys;

  public static Keys from(List<String> row, List<Integer> indices) {
    short size = (short) indices.size();
    val keys = new String[size];
    for (int index = 0; index < indices.size(); index++) {
      keys[index] = row.get(indices.get(index));
    }
    // TODO: checks
    return new Keys(size, keys);
  }

  /**
   * Somewhat optimized...
   */
  @Override
  public int compareTo(Keys keys) {
    // TODO: double-check for errors + guava way?
    if (size == 1) {
      val compared0 = this.keys[0].compareTo(keys.keys[0]);
      if (compared0 != 0) {
        return compared0;
      }
    } else if (size == 2) {
      val compared0 = this.keys[0].compareTo(keys.keys[0]);
      if (compared0 != 0) {
        return compared0;
      }
      val compared1 = this.keys[1].compareTo(keys.keys[1]);
      if (compared1 != 0) {
        return compared1;
      }
    } else if (size == 3) {
      val compared0 = this.keys[0].compareTo(keys.keys[0]);
      if (compared0 != 0) {
        return compared0;
      }
      val compared1 = this.keys[1].compareTo(keys.keys[1]);
      if (compared1 != 0) {
        return compared1;
      }
      val compared2 = this.keys[2].compareTo(keys.keys[2]);
      if (compared2 != 0) {
        return compared2;
      }
    }
    return 0;
  }

  @Value
  public static class Tuple {

    private final Keys pk;
    private final Keys fk;

    /**
     * Only applicable for some meta files
     */
    private final Keys secondaryFk;

    public boolean hasPk() {
      return pk != null;
    }

    public boolean hasFk() {
      return fk != null;
    }

    public boolean hasSecondaryFk() {
      return secondaryFk != null;
    }
  }
}