package org.icgc.dcc.genes;

import java.io.File;
import java.io.IOException;

public class Main {

  public static void main(String[] args) throws IOException {
    File bsonFile = new File("/Users/btiernay/Downloads/heliotrope-bson/dump/heliotrope/genes.bson");
    GenesLoader loader = new GenesLoader();
    loader.load(bsonFile);
  }

}
