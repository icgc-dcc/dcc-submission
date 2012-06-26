package org.icgc.dcc.validation;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.icgc.dcc.model.dictionary.Dictionary;
import org.icgc.dcc.model.dictionary.FileSchema;
import org.icgc.dcc.validation.plan.DefaultPlanner;
import org.icgc.dcc.validation.plan.LocalCascadingStrategy;

import cascading.cascade.Cascade;
import cascading.flow.Flow;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

public class Main {

  private final File root;

  private final File output;

  private final Dictionary dictionary;

  public Main(String[] args) throws JsonProcessingException, IOException {
    this.root = new File(args[0]);
    this.output = new File(args[1]);
    this.dictionary =
        new ObjectMapper().reader(Dictionary.class).readValue(
            Resources.toString(Main.class.getResource("/dictionary.json"), Charsets.UTF_8));
  }

  public static void main(String[] args) throws JsonProcessingException, IOException {
    new Main(args).doit();
  }

  private void doit() {
    if(output.exists() && output.listFiles() != null) {
      for(File f : output.listFiles()) {
        if(f.isFile()) {
          f.delete();
        }
      }
    }

    DefaultPlanner dp = new DefaultPlanner(new LocalCascadingStrategy(root, output));
    for(FileSchema fs : dictionary.getFiles()) {
      if(hasFile(fs)) {
        dp.prepare(fs);
      }
    }
    Cascade c = dp.plan();
    c.writeDOT(new File(output, "cascade.dot").getAbsolutePath());
    for(Flow flow : c.getFlows()) {
      flow.writeDOT(new File(output, flow.getName() + ".dot").getAbsolutePath());
    }
    c.start();
  }

  private boolean hasFile(final FileSchema fs) {
    File[] files = root.listFiles(new FileFilter() {

      @Override
      public boolean accept(File pathname) {
        return pathname.getName().contains(fs.getName());
        // return Pattern.matches(fs.getPattern(), pathname.getName());
      }
    });
    return files != null && files.length > 0;
  }
}
