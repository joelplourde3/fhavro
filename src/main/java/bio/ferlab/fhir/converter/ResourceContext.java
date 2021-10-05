package bio.ferlab.fhir.converter;

import bio.ferlab.fhir.schema.utils.Constant;
import ca.uhn.fhir.util.FhirTerser;
import ca.uhn.fhir.util.TerserUtilHelper;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.BaseResource;

import java.util.*;

import static bio.ferlab.fhir.converter.ConverterUtils.navigatePath;

public class ResourceContext {

    private final TerserUtilHelper helper;
    private final Deque<String> path;
    private final ArrayContext arrayState;

    public ResourceContext(TerserUtilHelper terserUtilHelper) {
        helper = terserUtilHelper;
        path = new ArrayDeque<>();
        arrayState = new ArrayContext();
    }

    public void addLastToPath(String value) {
        // Format value[x] in camelCase
        if (value.contains(Constant.VALUE) && value.length() >= 6) {
            value = value.substring(0, 5) + String.valueOf(value.charAt(5)).toUpperCase() + value.substring(6);
        }

        path.addLast(value);
    }

    public void detectPathConflict(Deque<String> path) {
        Iterator<String> iterator = path.iterator();
        String absolutePath = navigatePath(path);

        while (iterator.hasNext()) {
            String currentNode = iterator.next();
            if (getArrayContext().hasNode(currentNode)) {
                Base parent = (Base) getArrayContext().getCurrentBase(currentNode);
                List<IBase> children = getTerser().getValues(parent, navigatePath(path, true, path.size(), 1));
                getArrayContext().addNode(absolutePath, children);
                return;
            }
        }

        List<IBase> parents = getTerser().getValues(getResource(), absolutePath);
        if (!parents.isEmpty()) {
            getArrayContext().addNode(absolutePath, parents);
        }
    }

    public TerserUtilHelper getHelper() {
        return helper;
    }

    public FhirTerser getTerser() {
        return helper.getTerser();
    }

    public BaseResource getResource() {
        return helper.getResource();
    }

    public ArrayContext getArrayContext() {
        return arrayState;
    }

    public Deque<String> getPath() {
        return path;
    }

    public static class ArrayContext {
        private final Map<String, Node> nodes;

        public ArrayContext() {
            nodes = new HashMap<>();
        }

        public boolean hasNode(String path) {
            return nodes.containsKey(path);
        }

        public void addNode(String path, List<IBase> bases) {
            if (!hasNode(path)) {
                nodes.put(path, new Node(bases));
            } else {
                nodes.get(path).append(bases);
            }
        }

        public void progressNode(String path) {
            if (hasNode(path)) {
                this.nodes.get(path).progress();
            }
        }

        public IBase getCurrentBase(String path) {
            return this.nodes.get(path).getCurrentBase();
        }

        private static class Node {
            private final List<IBase> bases;
            private int index;

            protected Node(List<IBase> bases) {
                index = 0;
                this.bases = bases;
            }

            protected void progress() {
                index++;
            }

            protected IBase getCurrentBase() {
                return bases.get(index);
            }

            protected void append(List<IBase> bases) {
                this.bases.addAll(bases);
            }
        }
    }
}
