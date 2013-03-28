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
package org.icgc.dcc.test.mongodb;

import java.util.logging.Logger;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.mongodb.MongoClient;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfig;
import de.flapdoodle.embed.mongo.config.RuntimeConfig;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.io.NullProcessor;
import de.flapdoodle.embed.process.io.directories.FixedPath;

@Slf4j
public class EmbeddedMongo implements TestRule {

  private MongodExecutable mongodExe;

  private MongodProcess mongod;

  private MongodConfig mongodConfig;

  private MongoClient mongo;

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        log.info("Starting embedded Mongo...");
        start();
        log.info("Embedded Mongo started");

        base.evaluate();

        log.info("Stopping embedded Mongo...");
        stop();
        log.info("Embedded Mongo stopped");
      }
    };
  }

  public int getPort() {
    return mongodConfig.net().getPort();
  }

  public MongoClient getMongo() {
    return mongo;
  }

  @SneakyThrows
  private void start() {
    // Suppress logging
    System.setProperty("de.flapdoodle.embed.io.tmpdir", "target");
    RuntimeConfig runtimeConfig = RuntimeConfig.getInstance(Logger.getLogger(getClass().getName()));
    runtimeConfig.setProcessOutput(new ProcessOutput(new NullProcessor(), new NullProcessor(), new NullProcessor()));
    runtimeConfig.setTempDirFactory(new FixedPath("target"));

    // Start mongo
    MongodStarter runtime = MongodStarter.getInstance(runtimeConfig);
    mongodConfig = new MongodConfig(Version.Main.V2_3);
    mongodExe = runtime.prepare(mongodConfig);
    mongod = mongodExe.start();
    mongo = new MongoClient("localhost", mongodConfig.net().getPort());
  }

  @SneakyThrows
  private void stop() {
    // Stop mongo
    mongod.stop();
    mongodExe.stop();
  }

}
