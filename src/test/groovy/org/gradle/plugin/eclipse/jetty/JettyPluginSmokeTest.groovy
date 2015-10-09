package org.gradle.plugin.eclipse.jetty

import com.dsvdsv.gradle.plugin.eclipse.jetty.JettyPlugin
import com.dsvdsv.gradle.plugin.eclipse.jetty.JettyPluginConvention
import org.gradle.api.Project
import org.gradle.api.plugins.WarPlugin
import org.gradle.plugin.test.TestUtil
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test


/**
 * User: Dikansky
 * Date: 10.12.2014
 */
class JettyPluginSmokeTest {
	private Project project // = TestUtil.createRootProject()
	private JettyPlugin jettyPlugin

	@BeforeMethod
	public void setUp() {
		project = TestUtil.createRootProject()
		jettyPlugin = new JettyPlugin()
	}

	@Test
	public void testAppliesJavaPluginAndAddsConvention() {
		project.plugins.apply(WarPlugin)
		jettyPlugin.apply(project)

		Assert.assertEquals(project.convention.plugins.eclipseJetty.class, JettyPluginConvention)
		Assert.assertNotNull(project.getTasks().findByName('jettyRun'))
		Assert.assertNotNull(project.getTasks().findByName('jettyRunWar'))
		Assert.assertNotNull(project.getTasks().findByName('jettyStop'))
	}

	@Test
	public void testAppliesJavaPluginWithoutWarAndAddsConvention() {
		jettyPlugin.apply(project)

		Assert.assertNull(project.convention.plugins.eclipseJetty)
		Assert.assertNull(project.getTasks().findByName('jettyRun'))
		Assert.assertNull(project.getTasks().findByName('jettyRunWar'))
		Assert.assertNull(project.getTasks().findByName('jettyStop'))
	}
}
