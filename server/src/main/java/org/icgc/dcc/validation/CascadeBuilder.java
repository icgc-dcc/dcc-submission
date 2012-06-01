package org.icgc.dcc.validation;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Map;

import org.icgc.dcc.model.dictionary.FileSchema;

import cascading.pipe.Pipe;

import com.beust.jcommander.internal.Maps;

public class CascadeBuilder {

  private final Map<FileSchema, Pipe> assemblies = Maps.newHashMap();

  public CascadeBuilder(Iterable<FileSchema> heads) {
    checkArgument(heads != null);
    for(FileSchema head : heads) {
      assemblies.put(head, new Pipe(head.name));
    }
  }

  public void extend(FileSchema schema, PipeExtender extender) {
    assemblies.put(schema, extender.extend(assemblies.get(schema)));
  }

  /**
   * Extends a {@code Pipe}
   */
  interface PipeExtender {

    public Pipe extend(Pipe pipe);

  }

}
