package org.stefano.distributional.model.components.impl;

import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stefano.distributional.model.components.GraphWeigher;

import static java.util.Objects.requireNonNull;
import static org.stefano.distributional.model.components.GraphModel.CONTAINS;
import static org.stefano.distributional.model.components.GraphModel.FOLLOWS;

/**
 * This class provides an advanced method to weight the {@code FOLLOWS} relationships in a {@code word graph}.
 * This method generate weights that are inversely proportional to the frequency of paths scaled down by their length.
 */
public final class AdvancedGraphWeigher implements GraphWeigher {

    private static final Logger logger = LoggerFactory.getLogger(AdvancedGraphWeigher.class);

    @Override
    public void weight(GraphDatabaseService graph) {
        requireNonNull(graph, "'graph' is null");

        int total = 0;
        try (Transaction tx = graph.beginTx()) {
            long elapsed = System.nanoTime();
            logger.debug("Computing weights between words...");
            for (Relationship follows : graph.getAllRelationships()) {
                if (follows.isType(FOLLOWS)) {
                    Node tail = follows.getStartNode();
                    Node head = follows.getEndNode();
                    double freqTail = (double) tail.getProperty("freq", 1.0);
                    double freqHead = (double) head.getProperty("freq", 1.0);
                    double denom = 0.0;
                    for (Relationship containsTail : tail.getRelationships(CONTAINS, Direction.INCOMING)) {
                        int posTail = (int) containsTail.getProperty("pos", 0);
                        Node sentence = containsTail.getStartNode();
                        for (Relationship containsHead : sentence.getRelationships(CONTAINS, Direction.OUTGOING)) {
                            if (containsHead.getEndNode().equals(head)) {
                                int posHead = (int) containsHead.getProperty("pos", 0);
                                denom += 1.0 / (posHead - posTail);
                            }
                        }
                    }
                    double weight = (freqTail + freqHead) / denom;
                    weight = (weight) / (freqTail * freqHead);
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
