package org.stefano.distributional.model.components;

import org.neo4j.graphdb.GraphDatabaseService;

/**
 * This interface provide a method to weight the {@code FOLLOWS} relationships in a {@code word graph}.
 */
public interface GraphWeigher {

    /**
     * This method weights the {@code FOLLOWS} relationships in the given {@code graph}.
     *
     * @param graph the {@link GraphDatabaseService} whose {@code FOLLOWS} relationships have to be weighted
     */
    void weight(GraphDatabaseService graph);
}
