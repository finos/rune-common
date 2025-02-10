package org.finos.rune.mapper.processor;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.meta.ReferenceWithMeta;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.Processor;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GlobalReferenceCollector implements Processor {
    private final Set<GlobalReferenceRecord> globalReferences = new HashSet<>();

    @Override
    public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<? extends R> rosettaType, R instance, RosettaModelObject parent, AttributeMeta... metas) {
        if (instance instanceof ReferenceWithMeta) {
            @SuppressWarnings("unchecked")
            ReferenceWithMeta<R> reference = (ReferenceWithMeta<R>) instance;
            Class<?> referenceValueType = reference.getValueType();
            String referenceKeyValue = reference.getGlobalReference();
            globalReferences.add(new GlobalReferenceRecord(referenceValueType, referenceKeyValue));
        }
        return true;
    }

    @Override
    public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<? extends R> rosettaType, List<? extends R> instance, RosettaModelObject parent, AttributeMeta... metas) {
        System.err.println("processRosetta");
        return false;
    }

    @Override
    public <T> void processBasic(RosettaPath path, Class<? extends T> rosettaType, T instance, RosettaModelObject parent, AttributeMeta... metas) {
        System.err.println("processBasic");
    }

    @Override
    public <T> void processBasic(RosettaPath path, Class<? extends T> rosettaType, Collection<? extends T> instance, RosettaModelObject parent, AttributeMeta... metas) {
        System.err.println("processBasic collection");
    }

    public Set<GlobalReferenceRecord> getGlobalReferences() {
        return globalReferences;
    }

    @Override
    public Report report() {
        throw new UnsupportedOperationException("Report not supported for UnreferencedKeyCollector");
    }
}
