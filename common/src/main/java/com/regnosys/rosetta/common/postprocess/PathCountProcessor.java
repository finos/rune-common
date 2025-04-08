package com.regnosys.rosetta.common.postprocess;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2024 REGnosys
 * ==============
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

import com.regnosys.rosetta.common.util.SimpleProcessor;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.meta.Key;
import com.rosetta.model.lib.meta.Reference;
import com.rosetta.model.lib.meta.ReferenceWithMeta;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.metafields.MetaFields;

import java.util.*;

public class PathCountProcessor extends SimpleProcessor {

    private static final RosettaPath GLOBAL_REFERENCE = RosettaPath.valueOf("globalReference");
    private static final RosettaPath GLOBAL_KEY = RosettaPath.valueOf("globalKey");

    private final PathReport report = new PathReport();

    @Override
    public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<? extends R> rosettaType, R instance, RosettaModelObject parent, AttributeMeta... metas) {
        return instance != null && !isResolvedValueWithReference(instance, parent);
    }

    @Override
    public <T> void processBasic(RosettaPath path, Class<? extends T> rosettaType, Collection<? extends T> instances, RosettaModelObject parent, AttributeMeta... metas) {
        if (instances == null)
            return;
        int i = 0;
        for (T instance : instances) {
            processBasic(path.withIndex(i++), rosettaType, instance, parent, metas);
        }
    }

    @Override
    public <T> void processBasic(RosettaPath path, Class<? extends T> rosettaType, T instance, RosettaModelObject parent, AttributeMeta... metas) {
        if (instance == null || isGlobalKey(parent, path) || isGlobalReferenceKey(parent, path)) {
            return;
        }
        report.collectedPaths.put(path, instance);
    }

    private boolean isResolvedValueWithReference(RosettaModelObject instance, RosettaModelObject parent) {
        if (isResolvedValue(instance, parent)) {
            ReferenceWithMeta<?> referenceWithMeta = (ReferenceWithMeta<?>) parent;
            boolean isGlobalReferenceSet = referenceWithMeta.getGlobalReference() != null;
            boolean isExternalReferenceSet = referenceWithMeta.getExternalReference() != null;
            boolean isScopedReferenceSet = Optional.ofNullable(referenceWithMeta.getReference()).map(Reference::getReference).isPresent();
            return isScopedReferenceSet || isGlobalReferenceSet || isExternalReferenceSet;
        }
        return false;
    }

    
    
    private boolean isResolvedValue(RosettaModelObject instance, RosettaModelObject parent) {
        return parent instanceof ReferenceWithMeta && !(instance instanceof Reference);
    }

    private boolean isGlobalReferenceKey(RosettaModelObject parent, RosettaPath path) {
        return parent instanceof ReferenceWithMeta && path.endsWith(GLOBAL_REFERENCE);
    }

    private boolean isGlobalKey(RosettaModelObject parent, RosettaPath path) {
        return parent instanceof MetaFields && path.endsWith(GLOBAL_KEY);
    }

    @Override
    public PathReport report() {
        return report;
    }

    public static class PathReport implements Report {
        private final Map<RosettaPath, Object> collectedPaths = new HashMap<>();

        public Map<RosettaPath, Object> getCollectedPaths() {
            return collectedPaths;
        }
    }
}
