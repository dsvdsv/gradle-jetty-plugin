package com.dsvdsv.gradle.plugin.test

/**
 * User: Dikansky
 * Date: 10.12.2014
 */
class TestWorkspaceBuilder {
	def TestFile baseDir

	def TestWorkspaceBuilder(TestFile baseDir) {
		this.baseDir = baseDir
	}

	def apply(Closure cl) {
		cl.delegate = this
		cl.resolveStrategy = Closure.DELEGATE_FIRST
		cl()
	}

	def file(String name) {
		TestFile file = baseDir.file(name)
		file.write('some content')
		file
	}

	def setMode(int mode) {
		baseDir.mode = mode
	}

	def methodMissing(String name, Object args) {
		if (args.length == 1 && args[0] instanceof Closure) {
			baseDir.file(name).create(args[0])
		}
		else {
			throw new MissingMethodException(name, getClass(), args)
		}
	}
}

