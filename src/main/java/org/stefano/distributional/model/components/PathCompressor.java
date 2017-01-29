package org.stefano.distributional.model.components;

import org.neo4j.graphdb.*;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * This interface provides a method to generate a compressive summary from a {@code word graph}.
 */
public interface PathCompressor {

    PathExpander<Object> EXPANDER = PathExpanderBuilder.empty().add(GraphModel.FOLLOWS, Direction.OUTGOING).build();

    int MIN_DEPTH = 8;

    /**
     * Checks whether the given {@code path} contains at least a {@code verb}.
     *
     * @param path the {@link Path} to be checked
     * @return {@code true} if the given {@code path} contains a {@code verb}, {@code false} otherwise
     */
    static boolean hasVerb(Path path) {
        requireNonNull(path, "'path' is null");

        for (Node node : path.nodes()) {
            if (node.hasLabel(GraphModel.VERB)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Decodes the given {@code path} into a string and returns it.
     *
     * @param path the {@link Path} to be decoded
     * @return the (possibly empty) string from the given {@code path}
     */
    static Optional<String> decode(Path path) {
        requireNonNull(path, "'path' is null");

        String sentence = "";
        for (Node node : path.nodes()) {
            sentence = (sentence + " " + node.getProperty("word", "")).trim();
        }
        if (!sentence.isEmpty()) {
            return Optional.of(sentence + ".");
        }
        return Optional.empty();
    }

    /**
     * This method finds in the given {@code graph} all the paths from {@code start} to {@code end} that only use
     * {@code FOLLOWS} relationships and are no longer than the given {@code maxDepth}.
     * The paths that have no verbs or shorter than {@link this.MIN_DEPTH} are ignored because they likely lead
     * to poor summaries.
     * All the other paths are sorted by increasing cost, which is the sum of the (logarithmic) weights
     * on the {@code FOLLOWS} relationships of each path.
     * The minimal cost path most likely contains the most important concepts that summarises the graph
     * in a grammatically sounded way.
     * If such path exists, it is compressed to generate the summary to return.
     *
     * @param graph    the {@link GraphDatabaseService} with the {@code word graph} to be summarised
     * @param maxDepth the upper bound limit on the paths' length
     * @return the string that best summarises the graph, if any
     */
    Optional<String> compress(GraphDatabaseService graph, int maxDepth);

    /**
     * A {@link Path} associated with its {@code cost}.
     */
    final class CostPath implements Comparable<CostPath> {
        private final Path path;
        private final double cost;

        public CostPath(Path path, double cost) {
            this.path = requireNonNull(path, "'path' is null");
            this.cost = cost;
        }

        public Path getPath() {
            return path;
        }

        public double getCost() {
            return cost;
        }

        @Override
        public int compareTo(CostPath other) {
            requireNonNull(other, "'other' is null");

            int result = Double.compare(cost, other.cost);
            if (result == 0) {
                if (!path.equals(other.path)) {
                    result = Integer.compare(path.length(), other.path.length());
                    if (result == 0) {
                        result = (int) System.nanoTime() % 2;
                    }
                }
            }
            return result;
        }
    }
}
