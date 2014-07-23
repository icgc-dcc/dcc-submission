package org.icgc.dcc.submission.repository;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class RepositoryModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(UserRepository.class).in(Singleton.class);
    bind(ProjectRepository.class).in(Singleton.class);
    bind(ReleaseRepository.class).in(Singleton.class);
    bind(DictionaryRepository.class).in(Singleton.class);
    bind(CodeListRepository.class).in(Singleton.class);
    bind(ProjectDataTypeReportRepository.class).in(Singleton.class);
  }

}
