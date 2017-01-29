package org.stefano.distributional.utils;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * TODO Replace with proper description...
 * <p>
 * Created by stefano on 23/01/2017.
 */
public class OpenNLP {

    private static final Logger logger = LoggerFactory.getLogger(OpenNLP.class);
    private static SentenceDetector detector = null;
    private static Tokenizer tokenizer = null;
    private static POSTagger tagger = null;

    private OpenNLP() {
        throw new UnsupportedOperationException("'OpenNLP' class should not be instantiated");
    }

    public static SentenceDetector getSentenceDetector() {
        if (detector == null) {
            InputStream stream = OpenNLP.class.getResourceAsStream("/en-sent.bin");
            try {
                SentenceModel model = new SentenceModel(stream);
                detector = new SentenceDetectorME(model);
                logger.info("OpenNLP sentence detector lazily initialised");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return detector;
    }

    public static Tokenizer getTokenizer() {
        if (tokenizer == null) {
            InputStream stream = OpenNLP.class.getResourceAsStream("/en-token.bin");
            try {
                TokenizerModel model = new TokenizerModel(stream);
                tokenizer = new TokenizerME(model);
                logger.info("OpenNLP tokenizer lazily initialised");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return tokenizer;
    }

    public static POSTagger getPOSTagger() {
        if (tagger == null) {
            InputStream stream = OpenNLP.class.getResourceAsStream("/en-pos-maxent.bin");
            try {
                POSModel model = new POSModel(stream);
                tagger = new POSTaggerME(model);
                logger.info("OpenNLP POS tagger lazily initialised");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return tagger;
    }

}
