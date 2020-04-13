package com.regnosys.rosetta.common.testing;

public interface ExecutableFunction<INPUT, OUTPUT> {
    OUTPUT execute(INPUT input);
    Class<INPUT> getInputType();
    Class<OUTPUT> getOutputType();
}
