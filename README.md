# OpenNLP.Similarity Component

It is a project under Apache OpenNLP which subjects results of parsing, part-of-speech tagging and rhetoric parsing to machine learning.
It is leveraged in search, chat bots and other relevance assessment tasks

## What is OpenNLP.Similarity?

OpenNLP.Similarity is an NLP engine which solves a number of text processing and search tasks based on OpenNLP and Stanford NLP parsers. It is designed to be used by non-linguists to build linguistically-enabled 
<ul>
<li>search engines</li>
<li>recommendation systems</li>
<li>dialogue systems</li>
<li>text analysis and semantic processing engines</li>
<li>data-loss prevention system</li>
<li>text-based prediction engines</li>
<li>content & document generation tools</li>
<li>text writing style recognizers</li>
<li>general-purpose deterministic inductive learner</li>
</ul>

## Engines and Systems of OpenNLP.Similarity

### Main relevance assessment function
It takes two texts and returns the cardinality of a maximum common subgraph representations of these texts. This measure is supposed to be much more accurate than keyword statistics, compositional semantic models word2vec because linguistic structure is taken into account, not just co-occurrences of keywords. 
  [Matching class](https://github.com/bgalitsky/relevance-based-on-parse-trees/tree/master/src/main/java/opennlp/tools/parse_thicket/matching) has 
List<List<ParseTreeChunk>> assessRelevance(String para1, String para2)
function which returns the list of common phrases between these paragraphs.

It operates on the level of sentences (giving maximal common subtree) and paragraphs (giving maximal common [parse thicket](https://en.wikipedia.org/wiki/Parse_Thicket)).

<li>Search results re-ranker based on linguistic similarity</li>
<li>Request Handler for SOLR which used parse tree similarity</li>

### Search engine
<li>Search results re-ranker based on linguistic similarity</li>
<li>Request Handler for SOLR which used parse tree similarity</li>
<li>Taxonomy builder via learning from the web</li>
<li>appropriate rhetoric map of an answer</li>
<li>apply tree kernel learning to search relevance</li>

### Content generator
 It takes a topic, build a taxonomy for it, forms a table of content. It then  mines the web for documents for each table of content item, finds relevant sentences and paragraphs and [merges them into a document](https://github.com/bgalitsky/relevance-based-on-parse-trees/tree/master/src/main/java/opennlp/tools/similarity/apps). We attempt to reproduce how humans cut-and-paste content from the web while writing on a topic.
  Content generation has a [demo](http://37.46.135.20/)  and to run it from IDE start [here](https://github.com/bgalitsky/relevance-based-on-parse-trees/blob/master/src/main/java/opennlp/tools/similarity/apps/ContentGeneratorRunner.java). Examples of written documents are [here](http://37.46.135.20/wrt_latest/).
 
<li>Document builder with TOC, Sections, Figures& Captions and reference section</li>
<li>Review builder via taking a number of existing reviews and cross-breeding them, to write them so that they look "original"</li>

### Text detector / classifier
<li>Search results re-ranker based on linguistic similarity</li>
<li>Request Handler for SOLR which used parse tree similarity</li>
<li>Taxonomy builder via learning from the web</li>

### general-purpose [deterministic inductive learner](https://github.com/bgalitsky/relevance-based-on-parse-trees/tree/master/src/main/java/opennlp/tools/jsmlearning) implements JS Mills method of induction and abduction (deduction is also partially implemented).

## Related Research
Here's the link to the book on [question-answering](https://www.amazon.com/Natural-Language-Question-Answering-system/dp/0868039799/ref=sr_1_10?ie=UTF8&qid=1478871097&sr=8-10&keywords=galitsky)

and [research papers](https://scholar.google.com/citations?hl=ru&user=kR_M3HIAAAAJ).

Also the recent [book related to reasoning and linguistics in humans & machines](https://www.amazon.com/Computational-Autism-Human-Computer-Interaction-Galitsky/dp/3319399713)