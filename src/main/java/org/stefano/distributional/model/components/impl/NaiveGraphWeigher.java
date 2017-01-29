package org.stefano.distributional.model.components.impl;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stefano.distributional.model.components.GraphWeigher;

import static java.util.Objects.requireNonNull;
import static org.stefano.distributional.model.components.GraphModel.FOLLOWS;

/**
 * This class provides a naive method to weight the {@code FOLLOWS} relationships in a {@code word graph}.
 * This method generate weights that are inversely proportional to their frequency.
 */
public final class NaiveGraphWeigher implements GraphWeigher {

    private static final Logger logger = LoggerFactory.getLogger(NaiveGraphWeigher.class);

    @Override
    public void weight(GraphDatabaseService graph) {
        requireNonNull(graph, "'graph' is null");

        int total = 0;
        try (Transaction tx = graph.beginTx()) {
            long elapsed = System.nanoTime();
            logger.debug("Computing weights between words...");
            for (Relationship follows : graph.getAllRelationships()) {
                if (follows.isType(FOLLOWS)) {
                    double weight = 1.0 / (double) follows.getProperty("freq", 1.0);
                    follows.setProperty("weight", weight);
                    total += 1;
                    if (total % 50 == 0) {
                        logger.debug("{} relationships analysed so far...", total);
                    }
                }
            }
            elapsed = System.nanoTime() - elapsed;
            logger.info("{} relationship/s analysed in {} ms.",
                    total, String.format("%,.3f", elapsed / 1_000_000_000.0));
            tx.success();
        }
    }
}
