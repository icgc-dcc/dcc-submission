package org.icgc.dcc.genes.cli;

import com.beust.jcommander.IStringConverter;
import com.mongodb.MongoURI;

public class MongoURIConverter implements IStringConverter<MongoURI> {

  @Override
  public MongoURI convert(String uri) {
    return new MongoURI(uri);
  }

}