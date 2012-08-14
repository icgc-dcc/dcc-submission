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
package org.icgc.dcc.validation.cascading;

import java.util.ArrayList;
import java.util.List;

import cascading.tuple.TupleEntry;

/**
 * Offers various utils methods to handle {@code TupleEntry} (at least until we find cleaner cascading ways to do the
 * same, or that they offer more utils themselves)
 */
public class TupleEntryUtils {

  public static List<Object> getObjects(TupleEntry entry, String[] fields) {
    return getObjects(true, entry, fields);
  }

  public static List<Object> getNonNullObjects(TupleEntry entry, String[] fields) {
    return getObjects(false, entry, fields);
  }

  private static List<Object> getObjects(boolean all, TupleEntry entry, String[] fields) {
    List<Object> objects = new ArrayList<Object>();
    for(String field : fields) {
      Object object = entry.getObject(field);
      if(all || object != null) {
        objects.add(object);
      }
    }
    return objects;
  }

  public static boolean hasValues(TupleEntry entry, String[] fields) {
    return getNonNullObjects(entry, fields).isEmpty() == false;
  }
}
