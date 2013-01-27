package org.icgc.dcc.genes;

import java.io.File;

import lombok.ToString;

import org.icgc.dcc.genes.cli.FileValidator;
import org.icgc.dcc.genes.cli.MongoURIConverter;
import org.icgc.dcc.genes.cli.MongoURIValidator;
import org.icgc.dcc.genes.cli.MongoValidator;

import com.beust.jcommander.Parameter;
import com.mongodb.MongoURI;

/**
 * Command line options.
 * 
 * @author btiernay
 */
@ToString
public class Options {

  @Parameter(names = { "-f", "--file" }, required = true, validateValueWith = FileValidator.class, description = "Heliotrope genes.bson mongodump file (e.g. genes.bson)")
  public File file;

  @Parameter(names = { "-d", "--database" }, required = true, converter = MongoURIConverter.class, validateWith = MongoURIValidator.class, validateValueWith = MongoValidator.class, description = "DCC mongo database uri (e.g. mongodb://localhost/dcc-genes.Genes)")
  public MongoURI mongoUri;

  @Parameter(names = { "-v", "--version" }, help = true, description = "Show version information")
  public boolean version;

  @Parameter(names = { "-h", "--help" }, help = true, description = "Show help information")
  public boolean help;

}