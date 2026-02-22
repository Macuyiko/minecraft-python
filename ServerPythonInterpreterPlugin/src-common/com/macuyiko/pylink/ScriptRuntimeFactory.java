package com.macuyiko.pylink;

public interface ScriptRuntimeFactory {

	ScriptRuntime createRuntime(ScriptInstance script) throws Exception;

}
