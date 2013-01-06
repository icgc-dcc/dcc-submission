package org.icgc.dcc;

import org.junit.Test;

public class DataPortalServiceTest {

	@Test
	public void testMain() throws Exception {
		DataPortalService.main(new String[] { "server", "settings.yml" });
	}

}
