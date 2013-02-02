package org.icgc.dcc.genes.mongodb;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

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
public class EmbeddedMongo implements TestRule {

  private MongodExecutable mongodExe;
  private MongodProcess mongod;
  private MongodConfig mongodConfig;
  private Mongo mongo;

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        log.info("Starting Mongo...");
        start();

        log.info("Executing...");
        base.evaluate();

        log.info("Stopping Mongo...");
        stop();
      }
    };
  }

  public int getPort() {
    return mongodConfig.net().getPort();
  }

  public Mongo getMongo() {
    return mongo;
  }

  @SneakyThrows
  private void start() {
    // Configure
    System.setProperty("de.flapdoodle.embed.io.tmpdir", "target");
    RuntimeConfig runtimeConfig = new RuntimeConfig();
    runtimeConfig.setProcessOutput(new ProcessOutput(new NullProcessor(), new NullProcessor(), new NullProcessor()));
    runtimeConfig.setTempDirFactory(new FixedPath("target"));

    MongodStarter runtime = MongodStarter.getInstance(runtimeConfig);
    mongodConfig = new MongodConfig(Version.Main.V2_3);
    mongodExe = runtime.prepare(mongodConfig);
    mongod = mongodExe.start();
    mongo = new Mongo("localhost", mongodConfig.net().getPort());
  }

  @SneakyThrows
  private void stop() {
    mongod.stop();
    mongodExe.stop();
  }

}
