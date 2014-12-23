package org.gradle.plugin.eclipse.jetty

import org.eclipse.jetty.util.resource.Resource
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputDirectory

/**
 * User: dsv
 * Date: 18.08.13
 * Time: 20:00
 */
class JettyRunTask extends AbstractJettyTask {
	@InputDirectory File webAppSourceDirectory;
	FileCollection webAppClasses;
	File webXml;

	@Override
	protected void validateConfiguration() {
		if (getWebAppSourceDirectory()) {
			if (!getWebAppSourceDirectory().exists()) {
				throw new InvalidUserDataException(
						"Webapp source directory ${getWebAppSourceDirectory() == null ? "null" : getWebAppSourceDirectory().getCanonicalPath()} does not exist");
			} else {
				logger.info "Webapp source directory = ${getWebAppSourceDirectory().canonicalPath}"
			}
		}

		if (getWebAppClasses()) {
			if (getWebAppClasses().isEmpty()) {
				throw new InvalidUserDataException('webAppClasses does not empty');
			}
		}
	}

	public void configureWebApplication() throws Exception {
		super.configureWebApplication();
		Resource webAppSourceDirectoryResource = Resource.newResource(getWebAppSourceDirectory().getCanonicalPath());
		if (getWebAppConfig().getWar() == null) {
			getWebAppConfig().setWar(webAppSourceDirectoryResource.toString());
		}
		if (getWebAppConfig().getBaseResource() == null) {
			getWebAppConfig().setBaseResource(webAppSourceDirectoryResource);
		}

		if (getWebAppClasses() != null) {
			getWebAppConfig().setWebAppClasses(getWebAppClasses().files);
		}
		logger.info("Webapp directory = ${getWebAppSourceDirectory().getCanonicalPath()}");
	}
}
