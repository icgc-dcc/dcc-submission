package org.icgc.dcc.genes;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.mongodb.MongoURI;

/**
 * Command line utility used to import Heliotrope genes.bson {@code mongodump} file into DCC's MongoDB gene database.
 */
public class Main {

  private static final Logger log = LoggerFactory.getLogger(Main.class);

  @Parameter(names = { "-f", "--file" }, description = "Heliotrope genes.bson mongodump file", required = true)
  private File file;

  @Parameter(names = { "-d", "--database" }, converter = MongoURIConverter.class, description = "DCC mongo uri database")
  private MongoURI mongoUri = new MongoURI("mongodb://localhost");

  public static void main(String... args) throws IOException {
    Main main = new Main();
    new JCommander(main, args);

    main.load();
  }

  private void load() throws IOException {
    GenesLoader loader = new GenesLoader();

    log.info("Loading gene model using:");
    log.info("  file:     {}", file);
    log.info("  database: {}", mongoUri);
    loader.load(file, mongoUri);
    log.info("Finished loading", file);
  }

  private static class MongoURIConverter implements IStringConverter<MongoURI> {
    @Override
    public MongoURI convert(String value) {
      return new MongoURI(value);
    }
  }

}
