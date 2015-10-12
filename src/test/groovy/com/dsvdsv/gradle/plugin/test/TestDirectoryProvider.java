package com.dsvdsv.gradle.plugin.test;

/**
 * User: Dikansky
 * Date: 10.12.2014
 */
public interface TestDirectoryProvider {

	/**
	 * The directory to use, guaranteed to exist.
	 *
	 * @return The directory to use, guaranteed to exist.
	 */
	TestFile getTestDirectory();

}
