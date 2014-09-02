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
package org.icgc.dcc.core.util;

import static lombok.AccessLevel.PRIVATE;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import lombok.NoArgsConstructor;
import lombok.NonNull;

import org.icgc.dcc.core.model.FileTypes.FileSubType;
import org.icgc.dcc.core.model.FileTypes.FileType;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;

/**
 * Common optionals.
 */
@NoArgsConstructor(access = PRIVATE)
public class Optionals {

  public static final Optional<String> ABSENT_STRING = Optional.absent();
  public static final Optional<List<String>> ABSENT_STRING_LIST = Optional.absent();
  public static final Optional<Set<String>> ABSENT_STRING_SET = Optional.absent();
  public static final Optional<Map<String, String>> ABSENT_STRING_MAP = Optional.absent();
  public static final Optional<FileType> ABSENT_FILE_TYPE = Optional.absent();
  public static final Optional<FileSubType> ABSENT_FILE_SUB_TYPE = Optional.absent();

  /**
   * TODO: is there a way to do <O, T super O> rather?
   * <p>
   * TODO: try and make it reuse {@link Functions2#constant(Object)}.
   */
  public static <D, O extends D> Optional<D> cast(@NonNull final Optional<O> origin) {
    return cast(
        origin,
        new Function<O, D>() {

          @Override
          public D apply(O o) {
            return o;
          }

        });
  }

  public static <O, D> Optional<D> cast(
      @NonNull final Optional<O> origin,
      Function<O, D> castFunction) {
    return origin.isPresent() ?
        Optional.of(
            castFunction.apply(origin.get())) :
        Optional.<D> absent();
  }

  public static <K, V> Optional<Map<K, V>> of(@NonNull final Map<K, V> map) {
    return Optional.of(map);
  }

  public static <K, V> Optional<Entry<K, V>> of(@NonNull final Entry<K, V> entry) {
    return Optional.of(entry);
  }

  public static <T> Optional<Collection<T>> of(@NonNull final Collection<T> collection) {
    return Optional.of(collection);
  }

  public static <T> Optional<Iterable<T>> of(@NonNull final Iterable<T> iterable) {
    return Optional.of(iterable);
  }

  public static <T> Optional<List<T>> of(@NonNull final List<T> list) {
    return Optional.of(list);
  }

  public static <T> Optional<Set<T>> of(@NonNull final Set<T> set) {
    return Optional.of(set);
  }

  public final static <T> T defaultValue(
      @NonNull final Optional<T> optional,
      @NonNull final T defaultValue) {
    return defaultValue(
        optional,
        new Supplier<T>() {

          @Override
          public T get() {
            return defaultValue;
          }

        });
  }

  public final static <T> T defaultValue(
      @NonNull final Optional<T> optional,
      @NonNull final Supplier<T> supplier) {
    return optional.isPresent() ?
        optional.get() :
        supplier.get();
  }

  public final static <T, U> Optional<U> ofPredicate(
      @NonNull final Predicate<T> predicate,
      @NonNull final T t,
      @NonNull final Supplier<U> supplier) {
    return ofCondition(predicate.apply(t), supplier);
  }

  public final static <T> Optional<T> ofProposition(
      @NonNull final Proposition proposition,
      @NonNull final Supplier<T> supplier) {
    return ofCondition(proposition.evaluate(), supplier);
  }

  public final static <T> Optional<T> ofCondition(
      final boolean condition,
      @NonNull final Supplier<T> supplier) {
    return condition ?
        Optional.of(supplier.get()) :
        Optional.<T> absent();
  }

}
