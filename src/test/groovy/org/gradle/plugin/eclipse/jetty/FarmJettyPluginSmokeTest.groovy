package org.gradle.plugin.eclipse.jetty

import org.gradle.api.Project
import org.gradle.api.plugins.WarPlugin
import org.gradle.plugin.test.TestFile
import org.gradle.plugin.test.TestNameTestDirectoryProvider
import org.gradle.plugin.test.TestUtil
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * User: Dikansky
 * Date: 19.12.2014
 */
class FarmJettyPluginSmokeTest {
	private Project root
	private Project childWs;
	private Project childSite;

	TestFile childWsDir;
	TestFile childSiteDir;

	@BeforeMethod
	public void setUp() {
		childWsDir = TestNameTestDirectoryProvider.newInstance().testDirectory;
		childSiteDir = TestNameTestDirectoryProvider.newInstance().testDirectory;

		root = TestUtil.createRootProject();
		childWs = TestUtil.createChildProject(root, 'ws', childWsDir);
		childSite = TestUtil.createChildProject(root, 'site', childSiteDir);

		childWs.plugins.apply(WarPlugin)
		childWs.plugins.apply(JettyPlugin)

		childSite.plugins.apply(WarPlugin)
		childSite.plugins.apply(JettyPlugin)

		root.plugins.apply(JettyPlugin)

		childSiteDir.file("src/main/webapp").createDir()
		childWsDir.file("src/main/webapp").createDir()
	}

	@Test
	public void testAppliesJavaPluginAndAddsConvention() {
		Assert.assertEquals(childWs.convention.plugins.eclipseJetty.class, JettyPluginConvention)
		Assert.assertNotNull(childWs.getTasks().findByName('jettyRun'))
		Assert.assertNotNull(childWs.getTasks().findByName('jettyRunWar'))
		Assert.assertNotNull(childWs.getTasks().findByName('jettyStop'))

		Assert.assertEquals(childSite.convention.plugins.eclipseJetty.class, JettyPluginConvention)
		Assert.assertNotNull(childSite.getTasks().findByName('jettyRun'))
		Assert.assertNotNull(childSite.getTasks().findByName('jettyRunWar'))
		Assert.assertNotNull(childSite.getTasks().findByName('jettyStop'))

		Assert.assertNull(root.convention.plugins.eclipseJetty)
		Assert.assertNull(root.getTasks().findByName('jettyRun'))
		Assert.assertNull(root.getTasks().findByName('jettyRunWar'))
		Assert.assertNull(root.getTasks().findByName('jettyStop'))
		Assert.assertNotNull(root.getTasks().findByName('farmJettyRun'))
		Assert.assertNotNull(root.getTasks().findByName('farmJettyStop'))
	}

	@Test
	public void testCollectWebAppContextsConfig() {
		FarmJettyRunTask farmJettyRunTask = root.getTasks().findByName('farmJettyRun');
		def webAppsContexts = farmJettyRunTask.collectWebAppContexts()

		Assert.assertNotNull(webAppsContexts)
		Assert.assertEquals(webAppsContexts.size(), 2)
		Assert.assertNotNull webAppsContexts.find { x -> x.contextPath == '/site' }
		Assert.assertNotNull webAppsContexts.find { x -> x.contextPath == '/ws' }
	}
}
