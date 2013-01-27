package org.icgc.dcc.genes;

import java.io.File;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

/**
 * Command line utility used to import Heliotrope genes.bson {@code mongodump}
 * file into DCC's MongoDB gene database.
 */
@Slf4j
public class Main {

  private final Options options = new Options();

  public static void main(String... args) throws IOException {
    new Main().run(args);
  }

  private void run(String... args) throws IOException {
    JCommander cli = new JCommander(options);
    cli.setProgramName("java -jar " + getJarName());

    try {
      cli.parse(args);

      if (options.help) {
        cli.usage();

        return;
      } else if (options.version) {
        System.out.printf("ICGC DCC Gene Loader\nVersion %s\n", getVersion());

        return;
      }

      load();
    } catch (ParameterException pe) {
      System.err.printf("dcc-genes: %s\n", pe.getMessage());
      System.err.printf("Try `%s --help' for more information.\n", "java -jar " + getJarName());
    }
  }

  private void load() throws IOException {
    GenesLoader loader = new GenesLoader();

    log.info("Loading gene model using: {}", options);
    loader.load(options.file, options.mongoUri);
    log.info("Finished loading!");
  }

  private String getVersion() {
    String version = getClass().getPackage().getImplementationVersion();

    return version == null ? "" : version;
  }

  private String getJarName() {
    String jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
    File jarFile = new File(jarPath);

    return jarFile.getName();
  }

}
