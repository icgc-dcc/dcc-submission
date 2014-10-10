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
package org.icgc.dcc.submission.core.util;

import static lombok.AccessLevel.PRIVATE;
import lombok.NoArgsConstructor;

import org.icgc.dcc.common.core.model.DataType;
import org.icgc.dcc.common.core.model.DataType.DataTypes;
import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.mongodb.morphia.converters.SimpleValueConverter;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.MappingException;

@NoArgsConstructor(access = PRIVATE)
public final class TypeConverters {

  @SuppressWarnings("rawtypes")
  public static class FileTypeConverter extends TypeConverter implements SimpleValueConverter {

    @Override
    protected boolean isSupported(final Class c, final MappedField optionalExtraInfo) {
      return FileType.class.isAssignableFrom(c);
    }

    @Override
    public Object decode(final Class targetClass, final Object value, final MappedField optionalExtraInfo)
        throws MappingException {
      if (value == null) {
        return null;
      }

      return FileType.valueOf(value.toString());
    }

    @Override
    public Object encode(final Object value, final MappedField optionalExtraInfo) {
      if (value == null) {
        return null;
      }

      return ((FileType) value).name();
    }

  }

  @SuppressWarnings("rawtypes")
  public static class DataTypeConverter extends TypeConverter implements SimpleValueConverter {

    @Override
    protected boolean isSupported(final Class c, final MappedField optionalExtraInfo) {
      return DataType.class.isAssignableFrom(c);
    }

    @Override
    public Object decode(final Class targetClass, final Object value, final MappedField optionalExtraInfo)
        throws MappingException {
      if (value == null) {
        return null;
      }

      return DataTypes.valueOf(value.toString());
    }

    @Override
    public Object encode(final Object value, final MappedField optionalExtraInfo) {
      if (value == null) {
        return null;
      }

      return ((DataType) value).name();
    }

  }

}
