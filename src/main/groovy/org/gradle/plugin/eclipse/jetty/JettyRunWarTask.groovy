package org.gradle.plugin.eclipse.jetty

import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.InputFile

/**
 * User: dsv
 * Date: 18.08.13
 * Time: 20:00
 */
class JettyRunWarTask extends AbstractJettyTask {
	@InputFile File webApp

	@Override
	void validateConfiguration() {
		if (!getWebApp() || !getWebApp().exists()) {
			throw new InvalidUserDataException("Web application WAR ${getWebApp() == null ? 'null' : getWebApp().canonicalPath} does not exist")
		} else {
			logger.info "Web application WAR = ${getWebApp().canonicalPath}"
		}

	}

	@Override
	public void configureWebApplication() throws Exception {
		super.configureWebApplication();
		webAppConfig.setWar(getWebApp().canonicalPath);
	}
}
