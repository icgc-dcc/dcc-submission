package org.icgc.dcc.model.dictionary;

import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.annotations.PrePersist;

@Entity
public class Dictionary {

  @Id
  @JsonIgnore
  public ObjectId id;

  @Indexed(unique = true)
  public String version;

  public Date lastUpdate;

  public List<FileSchema> files;

  @PrePersist
  public void updateTimestamp() {
    lastUpdate = new Date();
  }

}
