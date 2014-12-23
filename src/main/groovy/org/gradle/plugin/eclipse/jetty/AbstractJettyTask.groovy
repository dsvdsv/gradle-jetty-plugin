package org.gradle.plugin.eclipse.jetty

import groovy.util.logging.Slf4j
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.logging.ProgressLogger
import org.gradle.logging.ProgressLoggerFactory

/**
 * User: dsv
 * Date: 25.08.13
 * Time: 19:48
 */
@Slf4j
abstract class AbstractJettyTask extends BaseJettyTask {
	@InputFiles
	Iterable<File> additionalRuntimeJars = new ArrayList<File>();
	JettyWebAppContext webAppConfig;
	String contextPath;
	@InputFile
	@Optional
	File jettyEnvXml;
	@InputFile
	@Optional
	File webDefaultXml;
	@InputFile
	@Optional
	File overrideWebXml;
	int scanIntervalSeconds;
	File jettyConfig;
	Integer stopPort;
	String stopKey;
	boolean daemon;
	Integer httpPort;
	def loginServices
	def requestLog;
	@InputFiles
	@Optional
	Iterable<File> extraResourceBases

	protected Scanner scanner;
	protected ArrayList<File> scanList;

	static final String PORT_SYSPROPERTY = "jetty.port";

	@Override
	protected void startJettyInternal() {
		ProgressLoggerFactory progressLoggerFactory = getServices().get(ProgressLoggerFactory.class);
		ProgressLogger progressLogger = progressLoggerFactory.newOperation(AbstractJettyTask.class);
		progressLogger.setDescription("Start Jetty server");
		progressLogger.setShortDescription("Starting Jetty");
		progressLogger.started();
		try {
			setServer(createServer());

			JettyServer server = getServer();
			getServer().configureConnector(getHttpPort());

			//set up the webapp and any context provided
			server.configureHandlers();
			configureWebApplication();
			server.addWebApplication(webAppConfig);

			//do any other configuration required by the
			//particular Jetty version
			if (getStopPort() != null && getStopPort() > 0 && getStopKey() != null) {
				server.enableShutdownMonitor(getStopPort(), getStopKey(), isDaemon());
			}

			// start Jetty
			server.start();

			if (isDaemon()) {
				return;
			}

			// start the new line scanner thread if necessary
			startConsoleScanner();

		} catch (Exception e) {
			throw new GradleException("Could not start the Jetty server.", e);
		} finally {
			progressLogger.completed();
		}

		progressLogger = progressLoggerFactory.newOperation(this.getClass());
		progressLogger.setDescription(String.format("Run Jetty at http://localhost:%d/%s", getHttpPort(), getContextPath()));
		progressLogger.setShortDescription(String.format("Running at http://localhost:%d/%s", getHttpPort(), getContextPath()));
		progressLogger.started();
		try {
			// keep the thread going if not in daemon mode
			server.join();
		} catch (Exception e) {
			throw new GradleException("Failed to wait for the Jetty server to stop.", e);
		} finally {
			progressLogger.completed();
		}
	}

	void configureWebApplication() {
		if (webAppConfig == null) {
			webAppConfig = new JettyWebAppContext();
		}
		webAppConfig.setContextPath(getContextPath().startsWith("/") ? getContextPath() : "/" + getContextPath());
		if (getTemporaryDir() != null) {
			webAppConfig.setTempDirectory(getTemporaryDir());
		}
		if (getWebDefaultXml() != null) {
			webAppConfig.setDefaultsDescriptor(getWebDefaultXml().getCanonicalPath());
		}
		if (getOverrideWebXml() != null) {
			webAppConfig.setOverrideDescriptor(getOverrideWebXml().getCanonicalPath());
		}
		if (getJettyEnvXml() != null) {
			webAppConfig.setJettyEnvXmlFile(getJettyEnvXml());
		}
		if (getExtraResourceBases() != null) {
			webAppConfig.setExtraResourceBases getExtraResourceBases().collect({ fileToString(it) });
		}
		Set<String> systemClasses = new LinkedHashSet<String>(Arrays.asList(webAppConfig.getSystemClasses()));
		systemClasses.remove('org.apache.commons.logging.');
		systemClasses.remove('org.apache.log4j.');
		webAppConfig.setSystemClasses(systemClasses.toArray(new String[systemClasses.size()]));

		webAppConfig.setParentLoaderPriority(false);

		logger.info "Context path = ${webAppConfig.getContextPath()}"
		logger.info 'Tmp directory =  determined at runtime'
		logger.info "Web defaults = " + (webAppConfig.getDefaultsDescriptor() == null ? " jetty default" : webAppConfig.getDefaultsDescriptor());
		logger.info "Web overrides = " + (webAppConfig.getOverrideDescriptor() == null ? " none" : webAppConfig.getOverrideDescriptor());
	}

	protected static fileToString(file) {
		file instanceof File ? file.absolutePath : file.toString()
	}
}
