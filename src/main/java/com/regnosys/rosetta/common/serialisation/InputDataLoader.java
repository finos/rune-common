package com.regnosys.rosetta.common.serialisation;

public interface InputDataLoader<T> {

    T loadInputFiles(T descriptor);
}
