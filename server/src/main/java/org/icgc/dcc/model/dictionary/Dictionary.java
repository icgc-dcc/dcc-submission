package org.icgc.dcc.model.dictionary;

import java.util.Date;
import java.util.List;

import org.icgc.dcc.model.BaseEntity;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.annotations.PrePersist;

@Entity
public class Dictionary extends BaseEntity {

  @Indexed(unique = true)
  public String version;

  public Date lastUpdate;

  public List<FileSchema> files;

  @PrePersist
  public void updateTimestamp() {
    lastUpdate = new Date();
  }

}
