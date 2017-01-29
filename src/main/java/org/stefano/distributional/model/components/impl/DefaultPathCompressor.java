package org.stefano.distributional.model.components.impl;

import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stefano.distributional.model.components.GraphModel;
import org.stefano.distributional.model.components.PathCompressor;

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * This class provides the default method to generate a compressive summary from a {@code word graph}.
 */
public final class DefaultPathCompressor implements PathCompressor {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPathCompressor.class);

    @Override
    public Optional<String> compress(GraphDatabaseService graph, int maxDepth) {
        try (Transaction tx = graph.beginTx()) {
            long elapsed = System.nanoTime();
            logger.debug("Computing all the paths between START and END nodes and their costs...");
            int total = 0;
            Set<CostPath> paths = new TreeSet<>();
            PathFinder<Path> finder = GraphAlgoFactory.allPaths(EXPANDER, maxDepth);
            for (Path path : finder.findAllPaths(GraphModel.start(graph), GraphModel.end(graph))) {
                if (path.length() >= PathCompressor.MIN_DEPTH && PathCompressor.hasVerb(path)) {
                    double cost = 0.0;
                    for (Relationship follows : path.relationships()) {
                        cost += (double) follows.getProperty("weight", 1.0);
                    }
                    paths.add(new CostPath(path, cost));
                }
                total += 1;
            }
            logger.info("{} valid path/s found (out of {} possible) in {} ms.",
                    paths.size(), total, String.format("%,.3f", elapsed / 1_000_000_000.0));
            if (paths.isEmpty()) {
                return Optional.empty();
            }
            logger.debug("Generating the compressive summary");
            return PathCompressor.decode(paths.iterator().next().getPath());
        }
    }

}
