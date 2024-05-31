package com.regnosys.rosetta.common.serialisation.mixin;

/*-
 * ==============
 * Rosetta Common
 * --------------
 * Copyright (C) 2018 - 2024 REGnosys
 * --------------
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==============
 */

/*-
 * #%L
 * Rosetta Common
 * %%
 * Copyright (C) 2018 - 2024 REGnosys
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.rosetta.model.lib.meta.ReferenceWithMeta;

//This class serves to ensure that the value of a reference doesn't get serialized if the
//reference or global key field is populated
public class ReferenceFilter extends SimpleBeanPropertyFilter {

    @Override
    public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider provider,
                                 PropertyWriter writer) throws Exception {
        if (!filterOut(pojo, writer.getName())) {
            writer.serializeAsField(pojo, jgen, provider);
        }
    }

    @Override
    public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider provider,
                                 BeanPropertyWriter writer) throws Exception {
        if (!filterOut(bean, writer.getName())) {
            writer.serializeAsField(bean, jgen, provider);
        }
    }

    private boolean filterOut(Object pojo, String name) {
        if (!name.equals("value")) return false;
        if (pojo instanceof ReferenceWithMeta) {
            return hasReference((ReferenceWithMeta<?>) pojo);
        }
        return false;
    }

    private boolean hasReference(ReferenceWithMeta<?> pojo) {
        return pojo.getGlobalReference() != null || (pojo.getReference() != null && pojo.getReference().getReference() != null);
    }
}
