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
package org.icgc.dcc.core.model;

import static com.google.common.collect.Iterables.transform;
import static java.util.Arrays.asList;
import static lombok.AccessLevel.PRIVATE;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public interface Identifiable {

  String getId();

  @NoArgsConstructor(access = PRIVATE)
  public static class Identifiables {

    @Deprecated
    public static <T> Identifiable fromClass(@NonNull final Class<T> type) {
      return new Identifiable() {

        @Override
        public String getId() {
          return type.getClass().getSimpleName(); // FIXME
        }

      };
    }

    public static Identifiable fromString(@NonNull final String s) {
      return new Identifiable() {

        @Override
        public String getId() {
          return s;
        }

      };
    }

    public static Identifiable fromInteger(@NonNull final Integer d) {
      return new Identifiable() {

        @Override
        public String getId() {
          return String.valueOf(d);
        }

      };
    }

    public static Iterable<Identifiable> fromStrings(@NonNull final String... qualifiers) {
      return transform(asList(qualifiers), Identifiables.fromStringFunction());
    }

    public static Function<Identifiable, String> getId() {
      return new Function<Identifiable, String>() {

        @Override
        public String apply(@NonNull final Identifiable identifiable) {
          return identifiable.getId();
        }

      };
    }

    public static Predicate<Identifiable> matches(@NonNull final String id) {
      return new Predicate<Identifiable>() {

        @Override
        public boolean apply(@NonNull final Identifiable identifiable) {
          return id.equals(identifiable.getId());
        }

      };
    }

    public static Identifiable[] toArray(Iterable<Identifiable> identifiables) {
      return Iterables.toArray(identifiables, Identifiable.class);
    }

    public static Function<String, Identifiable> fromStringFunction() {
      return new Function<String, Identifiable>() {

        @Override
        public Identifiable apply(@NonNull final String s) {
          return fromString(s);
        }

      };
    }

  }

}
