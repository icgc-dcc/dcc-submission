package org.icgc.dcc.submission.service;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class ServiceModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(ProjectService.class).in(Singleton.class);
		bind(ReleaseService.class).in(Singleton.class);
	}

}
