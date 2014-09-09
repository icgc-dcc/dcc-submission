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

import lombok.NoArgsConstructor;
import lombok.NonNull;

import com.google.common.base.Function;

/**
 * Utility methods for {@link Function}.
 */
@NoArgsConstructor(access = PRIVATE)
public final class Functions2 {

  public final static <T> Function<Integer, T> getValueAtIndex(@NonNull final List<T> list) {

    return new Function<Integer, T>() {

      @Override
      public T apply(@NonNull final Integer d) {
        return list.get(d);
      }

    };

  }

  public final static <T> Function<T, Integer> indexOf(@NonNull final List<T> list) {

    return new Function<T, Integer>() {

      @Override
      public Integer apply(@NonNull final T t) {
        return list.indexOf(t);
      }

    };

  }

  /**
   * Somehow guava's forces you to have {@link Object} as input.
   */
  public final static <T, C> Function<T, C> constant(@NonNull final C constant) {

    return new Function<T, C>() {

      @Override
      public C apply(T t) {
        return constant;
      }

    };

  }

  public static <T> Function<Collection<T>, Integer> size() {
    return new Function<Collection<T>, Integer>() {

      @Override
      public Integer apply(@NonNull final Collection<T> array) {
        return array.size();
      }

    };
  }

  public static <T> Function<T[], Integer> length() {
    return new Function<T[], Integer>() {

      @Override
      public Integer apply(@NonNull final T[] array) {
        return array.length;
      }

    };
  }

  public static Function<Integer, String> castIntegerToString() {
    return Functions2.<Integer> castToString();
  }

  public static <T> Function<T, String> castToString() {
    return new Function<T, String>() {

      @Override
      public String apply(T t) {
        return String.valueOf(t);
      }

    };
  }

  /**
   * TODO: how to specify "U super T"?
   */
  public static <T, U> Function<T, U> cast(@NonNull final Class<U> type) {
    return new Function<T, U>() {

      @Override
      public U apply(T t) {
        return type.cast(t);
      }

    };
  }

}
