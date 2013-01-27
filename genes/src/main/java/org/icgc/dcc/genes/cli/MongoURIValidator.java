package org.icgc.dcc.genes.cli;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;
import com.mongodb.MongoURI;

public class MongoURIValidator implements IParameterValidator {

  @Override
  public void validate(String name, String uri) throws ParameterException {
    try {
      new MongoURI(uri);
    } catch(IllegalArgumentException e) {
      throw new ParameterException("Invalid option: " + name + ": " + e.getMessage());
    }
  }

}
