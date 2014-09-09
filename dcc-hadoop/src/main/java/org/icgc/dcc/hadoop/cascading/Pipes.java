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
package org.icgc.dcc.hadoop.cascading;

import static com.google.common.collect.Iterables.toArray;
import static com.google.common.collect.Iterables.transform;
import static java.util.Arrays.asList;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.core.model.Identifiable.Identifiables.getId;
import static org.icgc.dcc.core.model.Identifiable.Identifiables.toArray;
import static org.icgc.dcc.core.util.Joiners.DASH;
import static org.icgc.dcc.core.util.Strings2.removeTrailingS;

import java.util.Collection;

import lombok.NoArgsConstructor;
import lombok.NonNull;

import org.icgc.dcc.core.model.Identifiable;
import org.icgc.dcc.core.model.Identifiable.Identifiables;
import org.icgc.dcc.core.util.Named;

import cascading.pipe.Pipe;

import com.google.common.base.Function;

/**
 * Utils methods for {@link Pipe}.
 */
@NoArgsConstructor(access = PRIVATE)
public class Pipes implements Named {

  private static final Pipes INTERNAL = new Pipes();
  private static final String CLASS_NAME = removeTrailingS(Pipes.class.getSimpleName());
  private static final Identifiable START_PIPE = Identifiables.fromString("start");
  private static final Identifiable END_PIPE = Identifiables.fromString("end");

  @Override
  public String getName() {
    return CLASS_NAME;
  }

  public static String getName(Iterable<Identifiable> identifiables) {
    return getName(toArray(identifiables));
  }

  public static String getName(Collection<Identifiable> identifiables) {
    return getName(toArray(identifiables));
  }

  public static String getName(Identifiable... identifiables) {
    return getNameInternal(transform(asList(identifiables), getId()));
  }

  private static String getNameInternal(Iterable<String> qualifiers) {
    return DASH.join(INTERNAL.getName(), DASH.join(qualifiers));
  }

  public static Iterable<String> getTailNames(final Pipe[] tails) {
    return getPipeNames(tails);
  }

  public static Iterable<String> getPipeNames(final Pipe[] pipes) {
    return transform(asList(pipes),
        new Function<Pipe, String>() {

          @Override
          public String apply(Pipe tail) {
            return tail.getName();
          }

        });
  }

  public static Pipe getStartPipe(@NonNull final String... qualifiers) {
    return getStartPipe(toArray(
        Identifiables.fromStrings(qualifiers),
        Identifiable.class));
  }

  public static Pipe getEndPipe(
      @NonNull final Pipe pipe,
      @NonNull final String... qualifiers) {
    return getEndPipe(
        pipe,
        toArray(
            Identifiables.fromStrings(qualifiers),
            Identifiable.class));
  }

  public static Pipe getStartPipe(@NonNull final Identifiable... identifiables) {
    return new Pipe(
        getName(
            Identifiables.fromString(
                getName(identifiables)),
            START_PIPE));
  }

  public static Pipe getEndPipe(
      @NonNull final Pipe pipe,
      @NonNull final Identifiable... identifiables) {
    return new Pipe(
        getName(
            Identifiables.fromString(
                getName(identifiables)),
            END_PIPE),
        pipe);
  }

}
