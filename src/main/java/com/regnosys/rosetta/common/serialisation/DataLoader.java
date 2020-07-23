package com.regnosys.rosetta.common.serialisation;

import java.util.List;

/**
 * Interface to lookup model related data from an external source. The data is typically a model instance that can be
 * loaded from a json source (e.g. file, rest api).
 */
public interface DataLoader<T> {
    List<T> load();

}
