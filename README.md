Multi-Sentence Compression
====

Compressing a cluster of related sentences into a single sentence that retains the most relevant non-redundant concepts from the original cluster and is grammatically sound is a complex task.

This project implements the method suggested in ["Multi-Sentence Compressing: Finding Shortest Paths in Word Graphs"](http://www.aclweb.org/anthology/C10-1037) which is based upon shortest paths in word graphs.

Specifically, we use:
* [OpenNLP](https://opennlp.apache.org) for basic sentence detection, tokenisation and POD tagging
* [Neo4j](https://neo4j.com) for graph generation and traversal
* Wikipedia's list of [most common words in English](https://en.wikipedia.org/wiki/Most_common_words_in_English.

Word graph
----

A `word graph` (or `adjacency text graph`) is a directed graph where:
* _words_ become **nodes** (punctuation is ignored)
* _adjacent words_ are connected by **edges** (type: _FOLLOWS_)
* _frequencies_ of words and adjacencies are saved on both **nodes** and **edges**.

Our data model includes a **node** to represent each _sentence_ (with _id_) and as many _CONTAINS_ **edges** as _words_ in each sentence (with their relative _pos_). The chain of _words_ of each sentence is also preceded by a _START_ **node** and followed by an _END_ **node**.

Given the following cluster of related sentences:

1. _The wife of a former U.S. president Bill Clinton, Hillary Clinton, visited China last Monday._
2. _Hillary Clinton wanted to visit China last month but postponed her plans till Monday last week._
3. _Hillary Clinton paid a visit to the People Republic of China on Monday._
4. _Last week the Secretary State Ms. Clinton visited Chinese officials._

the resulting `word graph` is presented below.

![Word graph for the example cluster](/images/word-graph.png)

