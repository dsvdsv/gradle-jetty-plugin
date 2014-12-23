package org.gradle.plugin.eclipse.jetty

import groovy.util.logging.Slf4j
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.logging.ProgressLogger
import org.gradle.logging.ProgressLoggerFactory

/**
 * User: Dikansky
 * Date: 19.12.2014
 */
@Slf4j
class FarmJettyRunTask extends BaseJettyTask {
	Integer stopPort;
	String stopKey;
	boolean daemon;
	Integer httpPort;

	Collection<JettyWebAppContext> collectWebAppContexts() {
		List<JettyWebAppContext> result = new ArrayList<>();
		Utils.projects(project).each {
			Project p ->
				if (Utils.isWebApp(p) && p.plugins.hasPlugin(JettyPlugin)) {
					JettyRunTask jettyRunTask = p.tasks.jettyRun;
					jettyRunTask.validateConfiguration();
					jettyRunTask.configureWebApplication();
					result.add(jettyRunTask.webAppConfig)
				}
		}
		return result;

	}

	@Override
	protected void validateConfiguration() {

	}

	@Override
	protected void startJettyInternal() {
		List<JettyWebAppContext> contexts = collectWebAppContexts();
		startJettyInternal(contexts);
	}

	void startJettyInternal(List<JettyWebAppContext> contexts) {
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

			contexts.each { x -> server.addWebApplication(x) }

			//do any other configuration required by the
			//particular Jetty version
			if (getStopPort() != null && getStopPort() > 0 && getStopKey() != null) {
				server.enableShutdownMonitor(getStopPort(), getStopKey(), isDaemon());
			}

			// start Jetty
			server.start();

			if (isDaemon()) {
				log.info('Daemon is ENABLED');
				return;
			} else {
				log.info('Daemon is DISABLED');
			}

			// start the new line scanner thread if necessary
			startConsoleScanner();

		} catch (Exception e) {
			throw new GradleException("Could not start the Jetty server.", e);
		} finally {
			progressLogger.completed();
		}
		def contextPaths = '';
		contexts.each { x -> contextPaths + ' ' + x.getContextPath() }
		progressLogger = progressLoggerFactory.newOperation(this.getClass());
		progressLogger.setDescription(String.format("Run Jetty at http://localhost:%d/%s", getHttpPort(), contextPaths));
		progressLogger.setShortDescription(String.format("Running at http://localhost:%d/%s", getHttpPort(), contextPaths));
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

}
