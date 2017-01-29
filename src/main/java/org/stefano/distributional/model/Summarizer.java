package org.stefano.distributional.model;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stefano.distributional.model.components.GraphEncoder;
import org.stefano.distributional.model.components.GraphWeigher;
import org.stefano.distributional.model.components.PathCompressor;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A facade for {@link GraphEncoder}, {@link GraphWeigher} and {@link PathCompressor} to summarise {@code sentences}.
 */
public final class Summarizer {

    private static final Logger logger = LoggerFactory.getLogger(Summarizer.class);

    private final Path folder;
    private final GraphEncoder encoder;
    private final GraphWeigher weigher;
    private final PathCompressor compressor;

    private Summarizer(SummarizerBuilder builder) {
        requireNonNull(builder, "'builder' is null");
        this.folder = builder.currentFolder;
        this.encoder = builder.currentEncoder;
        this.weigher = builder.currentWeigher;
        this.compressor = builder.currentCompressor;
    }

    /**
     * Returns a {@code builder} for {@link Summarizer}.
     *
     * @return a {@code builder} for {@link Summarizer}
     */
    public static RequiresFolder builder() {
        return new SummarizerBuilder();
    }

    /**
     * Process the given {@code sentences} with respect to the given {@code stopWords} and returns
     * the equivalent {@code multi-sentence compression}, if any.
     *
     * @param sentences the {@link List<String>} to compress
     * @param stopWords the {@link Collection<String>} of common words
     * @return the equivalent {@code multi-sentence compression}, if any
     */
    public Optional<String> process(List<String> sentences, Collection<String> stopWords) {
        requireNonNull(sentences, "'sentences' is null");
        requireNonNull(stopWords, "'stopWords' is null");

        if (sentences.isEmpty()) {
            return Optional.empty();
        }
        long elapsed = System.nanoTime();
        logger.debug("Compressing the following sentences:\n\t{}", String.join("\n\t", sentences));
        cleanup();
        GraphDatabaseService graph = new GraphDatabaseFactory().newEmbeddedDatabase(folder.toFile());
        int maxLength = encoder.encode(graph, sentences, stopWords);
        weigher.weight(graph);
        Optional<String> summary = compressor.compress(graph, maxLength);
        graph.shutdown();
        elapsed = System.nanoTime() - elapsed;
        logger.info("Compression completed in {} ms.", String.format("%,.3f", elapsed / 1_000_000_000.0));
        return summary;
    }

    private void cleanup() {
        long elapsed = System.nanoTime();
        logger.debug("Preparing database folder...");
        if (Files.notExists(folder)) {
            try {
                Files.createDirectories(folder);
            } catch (IOException e) {
                throw new IllegalArgumentException("'folder' can't be created: " + folder, e);
            }
        } else {
            try {
                Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                throw new IllegalArgumentException("'folder' can't be deleted: " + folder, e);
            }
        }
        elapsed = System.nanoTime() - elapsed;
        logger.debug("Database ready in {} ms.", String.format("%,.3f", elapsed / 1_000_000_000.0));
    }

    /**
     * An helper class to build a {@link Summarizer}.
     */
    public interface RequiresFolder {
        RequiresEncoder on(Path folder);
    }

    /**
     * An helper class to build a {@link Summarizer}.
     */
    public interface RequiresEncoder extends RequiresFolder {
        RequiresWeigher withEncoder(GraphEncoder encoder);
    }

    /**
     * An helper class to build a {@link Summarizer}.
     */
    public interface RequiresWeigher extends RequiresEncoder {
        RequiresCompressor withWeigher(GraphWeigher weigher);
    }

    /**
     * An helper class to build a {@link Summarizer}.
     */
    public interface RequiresCompressor extends RequiresWeigher {
        SummarizerBuilder withCompressor(PathCompressor compressor);
    }

    /**
     * An helper class to build a {@link Summarizer}.
     */
    public static class SummarizerBuilder implements RequiresCompressor {

        private Path currentFolder;
        private GraphEncoder currentEncoder;
        private GraphWeigher currentWeigher;
        private PathCompressor currentCompressor;

        private SummarizerBuilder() {
        }

        @Override
        public RequiresEncoder on(Path folder) {
            requireNonNull(folder, "'graph' is null");
            folder = folder.toAbsolutePath().normalize();
            if (Files.exists(folder) && !Files.isDirectory(folder)) {
                throw new IllegalArgumentException("'graph' is not a folder: " + folder);
            }
            currentFolder = folder;
            return this;
        }

        @Override
        public RequiresWeigher withEncoder(GraphEncoder encoder) {
            requireNonNull(encoder, "'encoder' is null");
            currentEncoder = encoder;
            return this;
        }

        @Override
        public RequiresCompressor withWeigher(GraphWeigher weigher) {
            requireNonNull(weigher, "'weigher' is null");
            currentWeigher = weigher;
            return this;
        }

        @Override
        public SummarizerBuilder withCompressor(PathCompressor compressor) {
            requireNonNull(compressor, "'compressor' is null");
            currentCompressor = compressor;
            return this;
        }

        public Summarizer build() {
            return new Summarizer(this);
        }
    }
}
