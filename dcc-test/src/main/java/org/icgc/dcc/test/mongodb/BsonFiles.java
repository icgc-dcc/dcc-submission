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
package org.icgc.dcc.test.mongodb;

import static lombok.AccessLevel.PRIVATE;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import lombok.Cleanup;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Predicate;

import de.undercouch.bson4jackson.BsonFactory;

@Slf4j
@NoArgsConstructor(access = PRIVATE)
public final class BsonFiles {

  @SneakyThrows
  public static File filterBsonFile(URL inputUrl, File outputFile, Predicate<ObjectNode> filter) {
    val mapper = new ObjectMapper(new BsonFactory());
    val reader = mapper.reader(JsonNode.class);
    val writer = mapper.writer();

    @Cleanup
    val outputWriter = new FileOutputStream(outputFile);

    log.info("Writing filtered BSON file from '{}' to '{}'", inputUrl, outputFile);
    val iterator = reader.readValues(inputUrl);
    while (hasNext(iterator)) {
      val record = (ObjectNode) iterator.next();
      if (filter.apply(record)) {
        outputWriter.write(writer.writeValueAsBytes(record));
      }
    }

    return outputFile;
  }

  private static boolean hasNext(MappingIterator<?> iterator) {
    try {
      return iterator.hasNextValue();
    } catch (IOException e) {
      return false;
    }
  }

}
