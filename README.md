Multi-Sentence Compression
====

Compressing a cluster of related sentences into a single sentence that retains the most relevant non-redundant concepts from the original cluster and is grammatically sound is a complex task.

This project implements the method suggested in ["Multi-Sentence Compressing: Finding Shortest Paths in Word Graphs"](http://www.aclweb.org/anthology/C10-1037) (**Katja Filippova.** Google Inc. _In Proc of 23rd Intl Conf COLING, 2010._) which is based upon shortest paths in word graphs.

Specifically, we use:
* [OpenNLP](https://opennlp.apache.org) for basic sentence detection, tokenisation and POD tagging
* [Neo4j](https://neo4j.com) for graph generation and traversal
* Wikipedia's list of [most common words in English](https://en.wikipedia.org/wiki/Most_common_words_in_English).

The procedure consists in:
* generating a `word graph`
* weighting the **edges** between _words_
* compressing the graph into a _meaningful summary_.

Word graph
----

A `word graph` (or `adjacency text graph`) is a directed graph where:
* _words_ become **nodes** (punctuation is ignored)
* _adjacent words_ are connected by **edges** (type: _FOLLOWS_)
* _frequencies_ of words and adjacencies are saved on both **nodes** and **edges**.

The **lower case text** and **POS tag** of each _word_ act as key, so that words with the same grammatical usage are unique in the graph.   
The only exception to this rule is for [stop-words](https://en.wikipedia.org/wiki/Most_common_words_in_English) which are always duplicated (if not involved in a _sintagmatic association_ with a relevant word) to keep their _frequencies_ (and importance in the graph) low.

Our data model also includes a **node** to represent each _sentence_ (with _id_) and as many _CONTAINS_ **edges** as _words_ in each sentence (with their relative _pos_). The chain of _words_ of each sentence is also preceded by a _START_ **node** and followed by an _END_ **node**.

Given the following cluster of related sentences:

1. _The wife of a former U.S. president Bill Clinton, Hillary Clinton, visited China last Monday._
2. _Hillary Clinton wanted to visit China last month but postponed her plans till Monday last week._
3. _Hillary Clinton paid a visit to the People Republic of China on Monday._
4. _Last week the Secretary State Ms. Clinton visited Chinese officials._

the resulting `word graph` is presented below.

![Word graph for the example cluster](/images/word-graph.png)

Weights
----

Both weight methods discussed in the [original paper](http://www.aclweb.org/anthology/C10-1037) have been implemented.

The **naive** method simply considers the inverse of the _frequency_ of each _FOLLOWS_ **edge**.

The **advanced** method is more sophisticated as it keeps into account **sintagmatic associations** scaled by 
the relative distance of the terms in their enclosing sentences.
 
In particular:
 
                            freq(i) + freq(j)
    w'(edge(i, j)) = ------------------------------
                      SUM(s in S) diff(s, i, j)^-1 

                    | pos(s, j) - pos(s, i)    if pos(s, i) < pos(s, j)
    diff(s, i, j) = | 
                    | 0                        otherwise

                       w'(edge(i, j))
    w"(edge(i, j) = -------------------
                     freq(i) x freq(j)

Notice that these weights are costs: the lower, the better.

Compression
----

The goal of this step is to generalise the input sentences by generating an appropriate compression (inductive task).
All the **paths** from _START_ to _END_ describe all the possible _worlds_ that can be reached upon summarisation.

In order to obtain sound summaries, we require paths to be at least **8 words** long and to contain **at least a verb**.
The remaining paths are ranked by **increasing cost**, which is the sum of the weights on their **edges** normalised by **path length**.

By visiting the _words_ in the **minimal cost path** (if any), the desired compression summary is generated.

Results
----

The project is organised as a [Gradle Application](https://docs.gradle.org/current/userguide/application_plugin.html), 
therefore it is sufficient to issue the following command on the terminal in the root folder of the project 
(provided that [Gradle]() is installed locally):

    gradle clean run

The example introduced above, for instance, produces the following output:

![Output for the example cluster](/images/output.png)

which includes the following summary: 

    Hillary Clinton wanted to visit China last week.

The algorithm has been successfully applied to English and Spanish by using an _ad-hoc_ **stop-word list** of 600 term ca.
The experimental results are discussed in the [original paper](http://www.aclweb.org/anthology/C10-1037).