package org.gradle.plugin.eclipse.jetty
/**
 * User: Dikansky
 * Date: 21.10.13
 */
public interface JettyServer {
	void start()

	void stop()

	void join();

	void configureHandlers()

	void addWebApplication(def context)

	void enableShutdownMonitor(int stopPort, String stopKey, boolean isDaemon)

	void configureConnector(int httpPort)
}