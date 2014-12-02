package org.gradle.plugin.eclipse.jetty

import org.gradle.api.file.FileCollection

/**
 * User: dsv
 * Date: 08.09.13
 * Time: 1:12
 */
class JettyPluginConvention {
	Integer stopPort = 8081;
	String stopKey = 'stop';
	Integer httpPort = 8080;
	File jettyEnvXml;
	Iterable<File> extraResourceBases;
}
