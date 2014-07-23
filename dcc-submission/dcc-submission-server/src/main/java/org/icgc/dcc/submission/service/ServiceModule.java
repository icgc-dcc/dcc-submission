package org.icgc.dcc.submission.service;

import com.google.inject.Singleton;

public class ServiceModule extends AbstractDccModule {

  @Override
  protected void configure() {
    bind(UserService.class).in(Singleton.class);
    bind(ProjectService.class).in(Singleton.class);
    bind(ReleaseService.class).in(Singleton.class);
    bind(DictionaryService.class).in(Singleton.class);
    bind(SystemService.class).in(Singleton.class);
    bind(MailService.class).in(Singleton.class);

    bindService(ExecutiveReportService.class);
    bindService(ValidationService.class);
  }

}
