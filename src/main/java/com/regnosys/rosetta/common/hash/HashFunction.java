package com.regnosys.rosetta.common.hash;

import java.util.function.BinaryOperator;

/**
 * Describes how to generate a hashcode for each basic type in Rosetta
 * @param <T> The type used for hash representation
 */
public interface HashFunction<T> {

    T identity();

    <U> T forBasicType(Class<U> basicType, U instance);

    BinaryOperator<T> accumulator();

}
