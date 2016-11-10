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
<li>general-purpose [deterministic inductive learner](https://github.com/bgalitsky/relevance-based-on-parse-trees/tree/master/src/main/java/opennlp/tools/jsmlearning)</li>

</ul>


## Engines and Systems of OpenNLP.Similarity

### Main relevance assessment function
It takes two texts and returns the cardinality of a maximum common subgraph representations of these texts. This measure is supposed to be much more accurate than keyword statistics, compositional semantic models word2vec because linguistic structure is taken into account, not just co-occurrences of keywords
<li>Search results re-ranker based on linguistic similarity</li>
<li>Request Handler for SOLR which used parse tree similarity</li>

### Search engine
<li>Search results re-ranker based on linguistic similarity</li>
<li>Request Handler for SOLR which used parse tree similarity</li>
<li>Taxonomy builder via learning from the web</li>

### Content generator
<li>Document builder with TOC, Sections, Figures& Captions and reference section</li>
<li>Review builder via taking a number of existing reviews and cross-breeding them, to write them so that they look "original"</li>
