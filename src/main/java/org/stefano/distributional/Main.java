package org.stefano.distributional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stefano.distributional.model.Summarizer;
import org.stefano.distributional.model.components.impl.AdvancedGraphWeigher;
import org.stefano.distributional.model.components.impl.DefaultGraphEncoder;
import org.stefano.distributional.model.components.impl.DefaultPathCompressor;
import org.stefano.distributional.utils.OpenNLP;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * TODO Replace with proper description...
 * <p>
 * Created by stefano on 23/01/2017.
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        Path folder = Paths.get(args[0]);

        List<String> sentences = Arrays.asList(
                "The wife of a former U.S. president Bill Clinton, Hillary Clinton, visited China last Monday.",
                "Hillary Clinton wanted to visit China last month but postponed her plans till Monday last week.",
                "Hillary Clinton paid a visit to the People Republic of China on Monday.",
                "Last week the Secretary State Ms. Clinton visited Chinese officials.");

        Collection<String> stopWords = Arrays.asList("a", "able", "about", "above", "after", "all", "also", "an",
                "and", "any", "as", "ask", "at", "back", "bad", "be", "because", "beneath", "big", "but", "by",
                "call", "can", "case", "child", "come", "company", "could", "day", "different", "do", "early", "even",
                "eye", "fact", "feel", "few", "find", "first", "for", "from", "get", "give", "go", "good",
                "government", "great", "group", "hand", "have", "he", "her", "high", "him", "his", "how", "i", "if",
                "important", "in", "into", "it", "its", "just", "know", "large", "last", "leave", "life", "like",
                "little", "long", "look", "make", "man", "me", "most", "my", "new", "next", "no", "not", "now",
                "number", "of", "old", "on", "one", "only", "or", "other", "our", "out", "over", "own", "part",
                "people", "person", "place", "point", "problem", "public", "right", "same", "say", "see", "seem",
                "she", "small", "so", "some", "take", "tell", "than", "that", "the", "their", "them", "then", "there",
                "these", "they", "thing", "think", "this", "time", "to", "try", "two", "under", "up", "us", "use",
                "want", "way", "we", "week", "well", "what", "when", "which", "who", "will", "with", "woman", "work",
                "world", "would", "year", "you", "young", "your");

        Summarizer summarizer = Summarizer.builder()
                .on(folder)
                .withEncoder(new DefaultGraphEncoder())
                .withWeigher(new AdvancedGraphWeigher())
                .withCompressor(new DefaultPathCompressor())
                .build();
        Optional<String> summary = summarizer.process(sentences, stopWords);
        if (summary.isPresent()) {
            System.out.println(" >> " +summary.get());
        } else {
            logger.info("No summary available.");
        }
        logger.info("Done.");
    }

}
