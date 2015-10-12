package com.dsvdsv.gradle.plugin.test

/**
 * User: Dikansky
 * Date: 10.12.2014
 */
class ExecOutput {
	ExecOutput(String rawOutput, String error) {
		this.rawOutput = rawOutput
		this.out = rawOutput.replaceAll("\r\n|\r", "\n")
		this.error = error
	}

	String rawOutput
	String out
	String error
}