package org.icgc.dcc.genes.cli;

import static com.google.common.base.Strings.isNullOrEmpty;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;
import com.mongodb.MongoURI;

public class MongoURIValidator implements IParameterValidator {

  @Override
  public void validate(String name, String uri) throws ParameterException {
    try {
      MongoURI mongoUri = new MongoURI(uri);

      String database = mongoUri.getDatabase();
      if(isNullOrEmpty(database)) {
        throw new ParameterException("Invalid option: " + name + ": uri must contain a database name");
      }

      String collection = mongoUri.getCollection();
      if(isNullOrEmpty(collection)) {
        throw new ParameterException("Invalid option: " + name + ": uri must contain a collection name");
      }
    } catch(IllegalArgumentException e) {
      throw new ParameterException("Invalid option: " + name + ": " + e.getMessage()
          + ". See http://docs.mongodb.org/manual/reference/connection-string/ for more information.");
    }
  }
}
