package com.dsvdsv.gradle.plugin.test

import org.apache.ivy.core.module.descriptor.Configuration
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor
import org.apache.ivy.core.module.id.ModuleId
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.codehaus.groovy.control.CompilerConfiguration
import org.gradle.api.Task
import org.gradle.api.internal.project.DefaultProject
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.internal.project.taskfactory.ITaskFactory
import org.gradle.groovy.scripts.DefaultScript
import org.gradle.groovy.scripts.Script
import org.gradle.groovy.scripts.ScriptSource
import org.gradle.testfixtures.ProjectBuilder

import java.rmi.server.UID

/**
 * User: Dikansky
 * Date: 10.12.2014
 */
class TestUtil {
	public static final Closure TEST_CLOSURE = {}

	static <T extends Task> T createTask(Class<T> type) {
		return createTask(type, createRootProject())
	}

	static <T extends Task> T createTask(Class<T> type, Map taskFields) {
		def task = createTask(type, createRootProject())
		hackInTaskProperties(type, task, taskFields)
		return task
	}

	private static void hackInTaskProperties(Class type, Task task, Map args) {
		args.each { k, v ->
			def field = type.getDeclaredFields().find { it.name == k }
			if (field) {
				field.setAccessible(true)
				field.set(task, v)
			} else {
				//I'm feeling lucky
				task."$k" = v
			}
		}
	}

	static <T extends Task> T createTask(Class<T> type, ProjectInternal project) {
		return createTask(type, project, 'name')
	}

	static <T extends Task> T createTask(Class<T> type, ProjectInternal project, String name) {
		return project.services.get(ITaskFactory).createTask([name: name, type: type])
	}

	static ProjectBuilder builder() {
		return ProjectBuilder.builder().withProjectDir(TestNameTestDirectoryProvider.newInstance().testDirectory)
	}

	static DefaultProject createRootProject() {
		createRootProject(TestNameTestDirectoryProvider.newInstance().testDirectory)
	}

	static DefaultProject createRootProject(File rootDir) {
		return ProjectBuilder
				.builder()
				.withProjectDir(rootDir)
				.build()
	}

	static DefaultProject createChildProject(DefaultProject parent, String name, File projectDir = null) {
		return ProjectBuilder
				.builder()
				.withName(name)
				.withParent(parent)
				.withProjectDir(projectDir)
				.build();
	}

	static DefaultModuleDescriptor createModuleDescriptor(Set confs) {
		DefaultModuleDescriptor moduleDescriptor = new DefaultModuleDescriptor(new ModuleRevisionId(new ModuleId('org', 'name'), 'rev'), "status", null)
		confs.each { moduleDescriptor.addConfiguration(new Configuration(it)) }
		return moduleDescriptor;
	}

	static groovy.lang.Script createScript(String code) {
		new GroovyShell().parse(code)
	}

	static Object call(String text, Object... params) {
		toClosure(text).call(*params)
	}

	static Closure toClosure(String text) {
		return new GroovyShell().evaluate("return " + text)
	}

	static Closure toClosure(ScriptSource source) {
		CompilerConfiguration configuration = new CompilerConfiguration();
		configuration.setScriptBaseClass(TestScript.getName());

		GroovyShell shell = new GroovyShell(configuration)
		Script script = shell.parse(source.resource.text)
		script.setScriptSource(source)
		return script.run()
	}

	static Closure toClosure(TestClosure closure) {
		return { param -> closure.call(param) }
	}

	static Closure returns(Object value) {
		return { value }
	}

	static Closure createSetterClosure(String name, String value) {
		return {
			"set$name"(value)
		}
	}

	static String createUniqueId() {
		return new UID().toString();
	}
}


interface TestClosure {
	Object call(Object param);
}

abstract class TestScript extends DefaultScript {
}

