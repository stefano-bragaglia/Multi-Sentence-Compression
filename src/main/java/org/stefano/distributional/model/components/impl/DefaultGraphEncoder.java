package org.stefano.distributional.model.components.impl;

import opennlp.tools.sentdetect.SentenceDetector;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stefano.distributional.model.components.GraphEncoder;
import org.stefano.distributional.model.components.GraphModel;
import org.stefano.distributional.utils.OpenNLP;

import java.util.*;

import static java.util.Objects.requireNonNull;
import static org.stefano.distributional.model.components.GraphModel.*;

/**
 * This class provides the default method to encode some {@code sentences} into a {@code word graph}.
 */
public final class DefaultGraphEncoder implements GraphEncoder {

    private static final Logger logger = LoggerFactory.getLogger(DefaultGraphEncoder.class);

    private static final SentenceDetector DETECTOR = OpenNLP.getSentenceDetector();

    @Override
    public int encode(GraphDatabaseService graph, List<String> sentences, Collection<String> stopWords) {
        requireNonNull(graph, "'graph' is null");
        requireNonNull(sentences, "'sentences' is null");
        requireNonNull(stopWords, "'stopWords' is null");

        int maxLength = 0;
        try (Transaction tx = graph.beginTx()) {
            long elapsed = System.nanoTime();
            logger.debug("Starting encoding...");
            int id = 0;
            for (String content : sentences) {
                for (String sentence : DETECTOR.sentDetect(content)) {
                    Token[] tokens = Token.parse(sentence);
                    logger.debug("Encoding sentence #{} ({} word/s; punctuation is ignored)...", id, tokens.length);
                    Node parent = graph.createNode(SENTENCE);
                    parent.setProperty("id", id++);
                    parent.setProperty("length", tokens.length);
                    int pos;
                    Node previous = GraphModel.start(graph);
                    for (pos = 0; pos < tokens.length; pos++) {
                        Node current = tokens[pos].isStopWord(stopWords) ?
                                getStopWord(graph, tokens, pos) :
                                getWord(graph, tokens, pos);
                        parent.createRelationshipTo(current, CONTAINS).setProperty("pos", pos);
                        GraphEncoder.link(previous, current);
                        previous = current;
                    }
                    GraphEncoder.link(previous, GraphModel.end(graph));
                    maxLength = Integer.max(pos, maxLength);
                }
            }
            elapsed = System.nanoTime() - elapsed;
            logger.info("Word graph generated in {} ms.",
                    String.format("%,.3f", elapsed / 1_000_000_000.0));
            tx.success();
        }
        return maxLength;
    }

    private Node getStopWord(GraphDatabaseService graph, Token[] tokens, int pos) {
        Label label = tokens[pos].getLabel();
        String text = tokens[pos].getText();
        ResourceIterator<Node> nodes = graph.findNodes(label, "text", text);
        if (nodes.hasNext()) {
            List<Context> contexts = new ArrayList<>();
            while (nodes.hasNext()) {
                Node node = nodes.next();
                Context context = getContext(graph, tokens, pos, node);
                if (!context.isEmpty()) {
                    contexts.add(context);
                }
            }
            if (!contexts.isEmpty()) {
                Collections.sort(contexts);
                Node node = contexts.get(0).getNode();
                node.setProperty("freq", (double) node.getProperty("freq", 1.0) + 1.0);
                return node;
            }
            return GraphEncoder.word(graph, tokens[pos], true);
        }
        return GraphEncoder.word(graph, tokens[pos], true);
    }

    private Node getWord(GraphDatabaseService graph, Token[] tokens, int pos) {
        Label label = tokens[pos].getLabel();
        String text = tokens[pos].getText();
        ResourceIterator<Node> nodes = graph.findNodes(label, "text", text);
        if (nodes.hasNext()) {
            List<Context> contexts = new ArrayList<>();
            while (nodes.hasNext()) {
                Node node = nodes.next();
                Context context = getContext(graph, tokens, pos, node);
                contexts.add(context);
            }
            Collections.sort(contexts);
            Node node = contexts.get(0).getNode();
            node.setProperty("freq", (double) node.getProperty("freq", 1.0) + 1.0);
            return node;
        }
        return GraphEncoder.word(graph, tokens[pos], false);
    }

    private Context getContext(GraphDatabaseService graph, Token[] tokens, int pos, Node node) {
        int count = 0;
        double freq = 0.0;
        for (Direction direction : new Direction[]{Direction.INCOMING, Direction.OUTGOING}) {
            Collection<String> texts = getTextsFromToken(tokens, pos, direction, 3);
            if (!texts.isEmpty()) {
                Map<String, Double> freqTexts = getTextsFromNode(node, direction, 3);
                texts.retainAll(freqTexts.keySet());
                count += texts.size();
                for (String t : texts) {
                    freq += freqTexts.getOrDefault(t, 1.0);
                }
            }
        }
        return new Context(node, count, freq);
    }

    private Collection<String> getTextsFromToken(Token[] tokens, int pos, Direction direction, int distance) {
        if (direction == Direction.BOTH) {
            return Collections.emptySet();
        }
        Set<String> result = new HashSet<>();
        int min = direction == Direction.INCOMING ?
                Integer.max(0, pos - distance) :
                pos + 1;
        int max = direction == Direction.INCOMING ?
                pos : Integer.min(tokens.length, pos + distance + 1);
        for (int i = min; i < max; i++) {
            result.add(tokens[i].getText());
        }
        return result;
    }

    private Map<String, Double> getTextsFromNode(Node node, Direction direction, int distance) {
        if (distance < 0) {
            return Collections.emptyMap();
        }
        Map<String, Double> result = new HashMap<>();
        for (Relationship relationship : node.getRelationships(FOLLOWS, direction)) {
            Node other = relationship.getOtherNode(node);
            String text = (String) other.getProperty("text", "");
            double freq = (double) other.getProperty("freq", 1.0);
            if (!text.isEmpty()) {
                result.put(text, result.getOrDefault(text, 1.0) + freq);
                if (distance > 1) {
                    Map<String, Double> map = getTextsFromNode(other, direction, distance - 1);
                    for (String mapText : map.keySet()) {
                        double mapFreq = map.get(mapText);
                        result.put(mapText, result.getOrDefault(mapText, 1.0) + mapFreq);
                    }
                }
            }
        }
        return result;
    }


}
