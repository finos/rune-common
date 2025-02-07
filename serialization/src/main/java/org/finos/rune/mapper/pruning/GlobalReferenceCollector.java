package org.finos.rune.mapper.pruning;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.Processor;

import java.util.*;


public class GlobalReferenceCollector implements Processor {
    //NOTE WE CAN'T KEY JUST WITH REFERENCE VALUES: a global key is a hash on only the value so to identify it you need it's type and key value
    private final Set<GlobalReferenceRecord> globalReferences = new HashSet<>();
    @Override
    public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<? extends R> rosettaType, R instance, RosettaModelObject parent, AttributeMeta... metas) {
        return false;
    }

    @Override
    public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<? extends R> rosettaType, List<? extends R> instance, RosettaModelObject parent, AttributeMeta... metas) {
        return false;
    }

    @Override
    public <T> void processBasic(RosettaPath path, Class<? extends T> rosettaType, T instance, RosettaModelObject parent, AttributeMeta... metas) {

    }

    @Override
    public <T> void processBasic(RosettaPath path, Class<? extends T> rosettaType, Collection<? extends T> instance, RosettaModelObject parent, AttributeMeta... metas) {

    }

    public Set<GlobalReferenceRecord> getGlobalReferences() {
        return globalReferences;
    }

    @Override
    public Report report() {
        throw new UnsupportedOperationException("Report not supported for UnreferencedKeyCollector");
    }

}
