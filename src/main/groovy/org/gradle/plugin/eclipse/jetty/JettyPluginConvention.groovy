package org.gradle.plugin.eclipse.jetty

import org.gradle.api.file.FileCollection

/**
 * User: dsv
 * Date: 08.09.13
 * Time: 1:12
 */
class JettyPluginConvention extends FarmJettyPluginConvention {
	File jettyEnvXml;
	Iterable<File> extraResourceBases;
}
