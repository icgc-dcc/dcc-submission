package org.icgc.dcc.genes;

import java.io.File;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.mongodb.MongoURI;

/**
 * Command line utility used to import Heliotrope genes.bson {@code mongodump}
 * file into DCC's MongoDB gene database.
 */
@Slf4j
public class Main {

  @Parameter(names = { "-f", "--file" }, required = true, validateValueWith = FileExistsValidator.class, description = "Heliotrope genes.bson mongodump file (e.g. genes.bson)")
  private File file;

  @Parameter(names = { "-d", "--database" }, required = true, converter = MongoURIConverter.class, description = "DCC mongo database uri (e.g. mongodb://localhost)")
  private MongoURI mongoUri;

  @Parameter(names = { "-v", "--version" }, help = true, description = "Show version information")
  private boolean version;
  
  @Parameter(names = { "-h", "--help" }, help = true, description = "Show help")
  private boolean help;
  
  public static void main(String... args) throws IOException {
    new Main().run(args);
  }

  private void run(String... args) throws IOException {
    JCommander cli = new JCommander(this);
    cli.setProgramName("java -jar " + getJarName());

    try {
      cli.parse(args);

      if (version) {
        System.out.println("ICGC DCC Gene Loader");
        System.out.printf("Version %s\n\n", getVersion());
        
        return;
      }      
      if (help) {
        cli.usage();
        
        return;
      }

      load();
    } catch (ParameterException pe) {
      System.err.printf("%s\n\n", pe.getMessage());
      cli.usage();
    }
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

  public static class FileExistsValidator implements IValueValidator<File> {
    public void validate(String name, File file) throws ParameterException {
      if (file.exists() == false) {
        throw new ParameterException(file.getAbsolutePath() + " does not exist");
      }
    }
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
