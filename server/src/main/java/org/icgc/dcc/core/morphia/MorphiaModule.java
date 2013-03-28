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
package org.icgc.dcc.core.morphia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.logging.MorphiaLoggerFactory;
import com.google.code.morphia.logging.slf4j.SLF4JLogrImplFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.typesafe.config.Config;

public class MorphiaModule extends AbstractModule {
  private static final Logger log = LoggerFactory.getLogger(MorphiaModule.class);

  @Override
  protected void configure() {
    // Use SLF4J with Morphia
    MorphiaLoggerFactory.reset();
    MorphiaLoggerFactory.registerLogger(SLF4JLogrImplFactory.class);

    bind(Morphia.class).toInstance(new Morphia());

    bind(Mongo.class).toProvider(new Provider<MongoClient>() {

      @Inject
      private Config config;

      @Override
      public MongoClient get() {
        try {
          String uri = config.getString("mongo.uri");
          log.info("mongo URI: {}", uri);
          return new MongoClientURI(uri).connect();
        } catch(Exception e) {
          throw new RuntimeException(e);
        }
      }
    }).in(Singleton.class);

    bind(Datastore.class).toProvider(new Provider<Datastore>() {

      @Inject
      Config config;

      @Inject
      Mongo mongo;

      @Inject
      Morphia morphia;

      @Override
      public Datastore get() {
        MongoClientURI uri = new MongoClientURI(config.getString("mongo.uri"));
        log.info("mongo URI: {}", uri);
        Datastore datastore = morphia.createDatastore(mongo, uri.getDatabase());
        datastore.ensureIndexes();
        return datastore;
      }
    }).in(Singleton.class);
  }

}
