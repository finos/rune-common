package com.regnosys.rosetta.common.inspection;

import com.rosetta.model.lib.RosettaModelObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.regnosys.rosetta.common.inspection.RosettaNodeInspector.Node;
import static com.regnosys.rosetta.common.inspection.RosettaReflectionsUtil.*;

public class PathObjectNode implements Node<PathObject<Object>> {

    static Predicate<Node<PathObject<Object>>> IS_NULL = (node) -> node.get() == null;

    public static PathObjectNode root(Object object) {
        return new PathObjectNode(new PathObject<>(object.getClass().getSimpleName(), object));
    }

    private final PathObject<Object> pathObject;

    public PathObjectNode(PathObject<Object> pathObject) {
        this.pathObject = pathObject;
    }

    @Override
    public List<Node<PathObject<Object>>> getChildren() {
        Object object = pathObject.getObject();
        return getAllPublicNoArgGetters(object.getClass()).stream()
                .map(method -> {
                    List<?> resultOfGetter = handleReturnTypes(invokeGetter(object, method));
                    List<PathObject<Object>> children = new ArrayList<>();
                    for (int i = 0; i < resultOfGetter.size(); i++) {
                        Object o = resultOfGetter.get(i);
                        children.add(Collection.class.isAssignableFrom(method.getReturnType()) ?
                                new PathObject<>(pathObject, attrName(method), i, o) :
                                new PathObject<>(pathObject, attrName(method), o));
                    }
                    return children;
                })
                .flatMap(List::stream)
                .map(PathObjectNode::new)
                .collect(Collectors.toList());
    }

    @Override
    public PathObject<Object> get() {
        return pathObject;
    }

    @Override
    public boolean isGuarded(Node<PathObject<Object>> childNode) {
        return IS_NULL.test(childNode);
    }

    @Override
    public boolean inspect() {
        return RosettaModelObject.class.isInstance(this.pathObject.getObject());
    }

    private String attrName(Method method) {
        String attrName = method.getName().replace("get", "");
        return Character.toLowerCase(attrName.charAt(0)) + attrName.substring(1);
    }
}