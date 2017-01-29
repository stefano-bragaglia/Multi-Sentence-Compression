package org.stefano.distributional.model.components;

import opennlp.tools.postag.POSTagger;
import opennlp.tools.tokenize.Tokenizer;
import org.neo4j.graphdb.*;
import org.stefano.distributional.utils.OpenNLP;

import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * This interface provides a method to encode some {@code sentences} into a {@code word graph}.
 */
public interface GraphEncoder {

    /**
     * Creates a {@code FOLLOWS} relationship between the given {@code tail} and {@code head} nodes
     * with {@code frequency} {@code 1.0} if no such relationship already exists, or updates
     * the {@code frequency} of the existing and eventually returns it.
     *
     * @param tail the start {@link Node} of the link to handle
     * @param head the end {@link Node} of the link to handle
     * @return the relationship between {@code tail} and {@code head} with updated {@code frequency}
     * if exists, a newly created relationship with {@code frequency} {@code 1.0} otherwise
     */
    static Relationship link(Node tail, Node head) {
        for (Relationship relationship : tail.getRelationships(GraphModel.FOLLOWS, Direction.OUTGOING)) {
            if (relationship.getOtherNode(tail).equals(head)) {
                double weight = (double) relationship.getProperty("freq", 1.0);
                relationship.setProperty("freq", 1.0 + weight);
                return relationship;
            }
        }
        Relationship relationship = tail.createRelationshipTo(head, GraphModel.FOLLOWS);
        relationship.setProperty("freq", 1.0);
        return relationship;
    }

    /**
     * Creates a {@link Node} in the given {@code graph} using the given {@code token} and {@code stopWord} flag.
     *
     * @param graph    the {@link GraphDatabaseService} where to create a node
     * @param token    the {@link Token} to convert into a node
     * @param stopWord a flag which tells if the node refers to a common word or not
     * @return the resulting {@link Node}
     */
    static Node word(GraphDatabaseService graph, Token token, boolean stopWord) {
        Label label = token.getLabel();
        Node node = graph.createNode(GraphModel.WORD, label);
        if (token.getTag().startsWith("VB")) {
            node.addLabel(GraphModel.VERB);
        }
        node.setProperty("text", token.getText());
        node.setProperty("word", token.getWord());
        node.setProperty("freq", 1.0);
        node.setProperty("stop", stopWord);
        return node;
    }


    /**
     * Encodes the given {@code sentences} as a {@code word graph} using the given {@code stopWords}
     * into the given {@code graph}, returning the length of the longest sentence.
     * Notice that punctuation is ignored and common words tend to build secondary paths.
     *
     * @param graph     the {@link GraphDatabaseService} where the given {@code sentences} are going to be saved
     * @param sentences the {@link List<String>} to be encoded into the given {@code graph}
     * @param stopWords the {@link Collection<String>} to identify common words
     * @return the number of words of the longest sentence among the given {@code sentences}
     */
    int encode(GraphDatabaseService graph, List<String> sentences, Collection<String> stopWords);

    /**
     * A {@code token} with (lower) text, word and POS tag.
     */
    final class Token {

        private static final Tokenizer TOKENIZER = OpenNLP.getTokenizer();
        private static final POSTagger TAGGER = OpenNLP.getPOSTagger();
        private static final Map<String, Label> LABELS = new HashMap<>();
        private final String text;
        private final String word;
        private final String tag;

        private Token(String token, String tag) {
            token = requireNonNull(token, "'token' is null").trim();
            if (token.isEmpty()) {
                throw new IllegalArgumentException("'token' is empty");
            }
            this.tag = requireNonNull(tag, "'tag' is null").trim();
            if (this.tag.isEmpty()) {
                throw new IllegalArgumentException("'tag' is empty");
            }
            this.text = token.toLowerCase();
            this.word = token;
        }

        public static Token[] parse(String sentence) {
            sentence = requireNonNull(sentence, "'sentence' is null").trim();
            if (sentence.isEmpty()) {
                throw new IllegalArgumentException("'sentence' is empty");
            }

            String[] tokens = TOKENIZER.tokenize(sentence);
            String[] tags = TAGGER.tag(tokens);
            List<Token> result = new ArrayList<>();
            for (int i = 0; i < tokens.length; i++) {
                if (isWord(tokens[i])) {
                    Token token = new Token(tokens[i], tags[i]);
                    result.add(token);
                }
            }
            return result.toArray(new Token[result.size()]);
        }

        private static boolean isWord(String symbol) {
            symbol = requireNonNull(symbol, "'symbol' is null").trim();
            if (symbol.isEmpty()) {
                throw new IllegalArgumentException("'symbol' is empty");
            }

            return symbol.matches("^(?=.*[\\p{L}\\p{N}'-]).+$");
        }

        public Label getLabel() {
            return LABELS.computeIfAbsent(tag, k -> Label.label(tag));
        }

        public String getText() {
            return text;
        }

        public String getWord() {
            return word;
        }

        public String getTag() {
            return tag;
        }

        public boolean isStopWord(Collection<String> stopWords) {
            requireNonNull(stopWords, "'stopWords' is null");

            return stopWords.contains(text);
        }

    }

    /**
     * A {@code context} for a {@code word} with {@code matches} and {@code occurrences}.
     */
    final class Context implements Comparable<Context> {

        private final Node node;

        private final int matches;

        private final double occurrences;

        public Context(Node node, int matches, double occurrences) {
            this.node = requireNonNull(node, "'node' is null");
            this.matches = matches;
            this.occurrences = occurrences;
        }

        public Node getNode() {
            return node;
        }

        public boolean isEmpty() {
            return matches <= 0;
        }

        @Override
        public int compareTo(Context other) {
            requireNonNull(other, "'other' is null");

            int result = Integer.compare(other.matches, this.matches);
            if (result == 0) {
                result = Double.compare(other.occurrences, this.occurrences);
                if (result == 0) {
                    if (node.equals(other.node)) {
                        result = 0;
                    } else {
                        result = (int) System.currentTimeMillis() % 2;
                    }
                }
            }
            return result;
        }
    }
}
