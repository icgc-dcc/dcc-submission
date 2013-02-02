package org.icgc.dcc.genes;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.junit.After;
import org.junit.Before;

import com.mongodb.Mongo;

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
public abstract class MongoIntegrationTest {

  private MongodExecutable mongodExe;
  private MongodProcess mongod;
  private MongodConfig mongodConfig;
  private Mongo mongo;

  @Before
  @SneakyThrows
  public void setUp() {
    // Configure
    log.info("Starting mongo...");
    RuntimeConfig runtimeConfig = new RuntimeConfig();
    runtimeConfig.setProcessOutput(new ProcessOutput(new NullProcessor(), new NullProcessor(), new NullProcessor()));
    runtimeConfig.setTempDirFactory(new FixedPath("target"));
    
    MongodStarter runtime = MongodStarter.getInstance(runtimeConfig);
    
    mongodConfig = new MongodConfig(Version.Main.V2_3);
    mongodExe = runtime.prepare(mongodConfig);
    mongod = mongodExe.start();
    mongo = new Mongo("localhost", mongodConfig.net().getPort());
  }

  @After
  @SneakyThrows
  public void tearDown() {
    log.info("Shutting down mongo...");
    mongod.stop();
    mongodExe.stop();
  }

  int getPort() {
    return mongodConfig.net().getPort();
  }

  Mongo getMongo() {
    return mongo;
  }

}
