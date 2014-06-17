package org.icgc.dcc.reporter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

  private static final String PARENT_DIR_PARAMETER = "parent_dir";

  public static void main(String[] args) {

    new Reporter().report("my_release", InputData.getDummy());
  }

}
