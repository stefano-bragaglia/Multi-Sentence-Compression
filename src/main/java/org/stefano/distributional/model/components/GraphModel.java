package org.stefano.distributional.model.components;

import org.neo4j.graphdb.*;

import static java.util.Objects.requireNonNull;

/**
 * Definitions for {@code word graphs}.
 */
public class GraphModel {

    public static final Label SENTENCE = Label.label("SENTENCE");
    public static final Label START = Label.label("START");
    public static final Label WORD = Label.label("WORD");
    public static final Label END = Label.label("END");
    public static final Label VERB = Label.label("VERB");
    public static final RelationshipType FOLLOWS = RelationshipType.withName("FOLLOWS");
    public static final RelationshipType CONTAINS = RelationshipType.withName("CONTAINS");

    private GraphModel() {
        throw new UnsupportedOperationException("'GraphModel' class should not be instantiated");
    }

    /**
     * Returns the {@code START} node in the given {@code graph}.
     * If such node exists, its {@code frequency} is increased by 1 and eventually returned.
     * If it doesn't, the node is created and initialised to {@code frequency} == 1 and returned.
     *
     * @param graph the target {@link GraphDatabaseService}
     * @return the updated {@code START} node, or a newly created instance
     */
    public static Node start(GraphDatabaseService graph) {
        requireNonNull(graph, "'graph' is null");

        return terminal(graph, START);
    }

    /**
     * Returns the {@code END} node in the given {@code graph}.
     * If such node exists, its {@code frequency} is increased by 1 and eventually returned.
     * If it doesn't, the node is created and initialised to {@code frequency} == 1 and returned.
     *
     * @param graph the target {@link GraphDatabaseService}
     * @return the updated {@code END} node, or a newly created instance
     */
    public static Node end(GraphDatabaseService graph) {
        requireNonNull(graph, "'graph' is null");

        return terminal(graph, END);
    }

    private static Node terminal(GraphDatabaseService graph, Label label) {
        requireNonNull(graph, "'graph' is null");

        ResourceIterator<Node> nodes = graph.findNodes(label);
        if (nodes.hasNext()) {
            Node node = nodes.next();
            double freq = (double) node.getProperty("freq", 1.0);
            node.setProperty("freq", 1.0 + freq);
            return node;
        }
        Node node = graph.createNode(label);
        node.setProperty("freq", 1.0);
        return node;
    }

}
