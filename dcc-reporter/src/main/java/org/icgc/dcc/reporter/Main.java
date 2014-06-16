package org.icgc.dcc.reporter;

public class Main {

  public static void main(String[] args) {
    new Reporter().report("my_release", InputData.getDummy());
  }

}
