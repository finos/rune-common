package com.regnosys.rosetta.common.serialisation;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface ObjectMapperCreator {

    ObjectMapper create();

}
