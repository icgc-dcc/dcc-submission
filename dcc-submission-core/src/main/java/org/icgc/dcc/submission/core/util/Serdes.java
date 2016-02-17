/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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

import java.io.IOException;

import lombok.NoArgsConstructor;
import lombok.val;

import org.icgc.dcc.common.core.model.DataType;
import org.icgc.dcc.common.core.model.DataType.DataTypes;
import org.icgc.dcc.common.core.model.FileTypes.FileType;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

@NoArgsConstructor(access = PRIVATE)
public final class Serdes {

  public static class FileTypeSerializer extends JsonSerializer<FileType> {

    @Override
    public void serialize(FileType value, JsonGenerator generator, SerializerProvider provider)
        throws IOException, JsonProcessingException {
      generator.writeObject(value.name());
    }
  }

  public static class FileTypeDeserializer extends JsonDeserializer<FileType> {

    @Override
    public FileType deserialize(JsonParser parser, DeserializationContext context) throws IOException,
        JsonProcessingException {
      val value = parser.getText();

      return FileType.valueOf(value);
    }

  }

  public static class DataTypeSerializer extends JsonSerializer<DataType> {

    @Override
    public void serialize(DataType value, JsonGenerator generator, SerializerProvider provider)
        throws IOException, JsonProcessingException {
      generator.writeObject(value.name());
    }
  }

  public static class DataTypeDeserializer extends JsonDeserializer<DataType> {

    @Override
    public DataType deserialize(JsonParser parser, DeserializationContext context) throws IOException,
        JsonProcessingException {
      val value = parser.getText();

      return DataTypes.valueOf(value);
    }

  }

}
