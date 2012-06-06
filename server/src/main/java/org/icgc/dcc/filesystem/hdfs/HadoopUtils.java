package org.icgc.dcc.filesystem.hdfs;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.hadoop.conf.Configuration;

public class HadoopUtils {

  public static String getConfigurationDescription(Configuration configuration) throws IOException {
    final Writer writer = new StringWriter();
    final PrintWriter printWriter = new PrintWriter(writer);
    Configuration.dumpConfiguration(configuration, printWriter);
    String content = writer.toString();
    printWriter.close();
    writer.close();
    return content;
  }
}
