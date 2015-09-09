package org.wildfly.apigen.invocation;

import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.jandex.*;
import org.wildfly.apigen.model.AddressTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * @author Lance Ball
 */
public class Marshaller {

    private static HashMap<Class<?>, EntityAdapter<?>> adapters = new HashMap<>();
    private static HashMap<Class<?>, Optional<Subresource>> subresources = new HashMap<>();

    public static LinkedList<ModelNode> marshal(Object root) throws Exception {
        return appendNode(root, PathAddress.EMPTY_ADDRESS, new LinkedList<>());
    }

    @SuppressWarnings("unchecked")
    private static LinkedList<ModelNode> appendNode(Object entity, PathAddress address, LinkedList<ModelNode> list) throws Exception {
        final PathAddress resourceAddress = resourceAddress(entity, address);
        final ModelNode modelNode = addressNodeFor(resourceAddress);

        EntityAdapter adapter = adapterFor(entity.getClass());
        list.add( adapter.fromEntity(entity, modelNode) );

        return marshalSubresources(entity, resourceAddress, list);
    }

    private static PathAddress resourceAddress(Object resource, PathAddress pathAddress) {
        final Class<?> entityClass = resource.getClass();

        Index index = IndexFactory.createIndex(entityClass);
        ClassInfo clazz = index.getClassByName(DotName.createSimple(entityClass.getName()));

        for (AnnotationInstance annotation :  clazz.classAnnotations()) {
            if (annotation.name().equals(IndexFactory.ADDRESS_META)) {
                AddressTemplate address = AddressTemplate.of(annotation.value().asString());
                String name = address.getResourceName();
                if (name.equals("*") && clazz.method("getKey") != null) {
                    try {
                        name = (String) entityClass.getMethod("getKey").invoke(resource);
                        if (name == null) name = address.getResourceName();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                }
                pathAddress = pathAddress.append(address.getResourceType(), name);
                return pathAddress;
            }
        }
        throw new RuntimeException("Cannot determine resource address for " + resource);
    }

    private static ModelNode addressNodeFor(PathAddress address) {
        ModelNode node = new ModelNode();
        node.get(OP_ADDR).set(address.toModelNode());
        node.get(OP).set(ADD);
        return node;
    }

    private static synchronized EntityAdapter adapterFor(Class<?> type) {
        if (!adapters.containsKey(type)) {
            adapters.put(type, new EntityAdapter<>(type));
        }
        return adapters.get(type);
    }

    public static synchronized Optional<Subresource> subresourcesFor(Object entity) {
        Class<?> type = entity.getClass();
        if (!subresources.containsKey(type)) {
            try {
                Method target = type.getMethod("subresources");
                subresources.put(type, Optional.of(new Subresource(target.getReturnType(), target, entity)));
            } catch (Exception e) {
                // If no subresources() method, then no subresources exist
                subresources.put(type, Optional.empty());
            }
        }
        return subresources.get(type);
    }

    private static LinkedList<ModelNode> singletonSubresourcesFor(Object entity, PathAddress address) throws Exception {
        final LinkedList<ModelNode> list = new LinkedList<>();
        for(Method target : orderedSubresources(entity)) {
            final Object result = target.invoke(entity);
            if (result != null) appendNode(result, address, list);
        }
        return list;
    }

    private static List<Method> orderedSubresources(Object parent) throws NoSuchMethodException {
        final Class<?> parentClass = parent.getClass();
        return new SubresourceFilter(parentClass).invoke();
    }

    private static LinkedList<ModelNode> marshalSubresources(Object parent, PathAddress address, LinkedList<ModelNode> list) {
        try {
            // First handle singletons
            list.addAll(singletonSubresourcesFor(parent, address));

            // Now handle lists
            Optional<Subresource> optional = subresourcesFor(parent);

            if (optional.isPresent()) {
                Object subresources = optional.get().invoke();

                for(Method target : orderedSubresources(subresources)) {
                    List<?> resourceList = (List<?>) target.invoke(subresources);
                    for (Object o : resourceList) {
                        appendNode(o, address, list);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting subresources for " + parent.getClass().getSimpleName());
            e.printStackTrace();
        }
        return list;
    }

    private static class Subresource {
        public final Class<?> type;
        public final Method method;
        private final Object parent;

        public Subresource(Class<?> type, Method method, Object parent) {
            this.type = type;
            this.method = method;
            this.parent = parent;
        }

        public Object invoke() throws InvocationTargetException, IllegalAccessException {
            return method.invoke(parent);
        }

    }

}

