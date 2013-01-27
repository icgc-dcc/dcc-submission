package org.icgc.dcc.genes.cli;

import java.io.File;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;

public class FileValidator implements IValueValidator<File> {

  public void validate(String name, File file) throws ParameterException {
    if (file.exists() == false) {
      throw new ParameterException("Invalid option: " + name + ": " + file.getAbsolutePath() + " does not exist");
    }
  }
}