package org.gradle.plugin.eclipse.jetty

import org.gradle.api.Project
import org.gradle.plugin.test.TestUtil
import org.testng.Assert
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test


/**
 * User: Dikansky
 * Date: 10.12.2014
 */
class JettyPluginTest {
	private Project project // = TestUtil.createRootProject()
	private JettyPlugin jettyPlugin

	@BeforeClass
	public void setUp() {
		project = TestUtil.createRootProject()
		jettyPlugin = new JettyPlugin()
	}

	@Test
	public void appliesJavaPluginAndAddsConvention() {
		jettyPlugin.apply(project)

		Assert.assertEquals(project.convention.plugins.eclipseJetty.class, JettyPluginConvention)
		Assert.assertNotNull(project.getTasks().findByName('jettyRun'))
		Assert.assertNotNull(project.getTasks().findByName('jettyRunWar'))
		Assert.assertNotNull(project.getTasks().findByName('jettyStop'))
	}
}
