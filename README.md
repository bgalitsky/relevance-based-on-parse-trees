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
<li>content & document generation tools</li>
<li>text writing style recognizers</li>
<li>general-purpose deterministic inductive learner</li>
</ul>

## Installation
 0) Do ['git clone'](https://github.com/bgalitsky/relevance-based-on-parse-trees.git) to setup the environment including resources. Besides git, /resources directory requires some additional work
 
 1) Download the main [jar](https://github.com/bgalitsky/relevance-based-on-parse-trees/blob/master/opennlp-similarity.11.jar).
 
 2) Set all necessary jars in /lib folder. Larger size jars are not on git so please download them from [Stanford NLP site](http://nlp.stanford.edu/)
 <li>edu.mit.jverbnet-1.2.0.jar</li>
 <li>ejml-0.23.jar</li>
 <li>joda-time.jar</li>
 <li>jollyday.jar</li>
 <li>stanford-corenlp-3.5.2-models.jar</li>
 <li>xom.jar</li>
 The rest of jars are available via maven.
 
 3) Set up src/test/resources directory
  - new_vn.zip needs to be unzipped
  - OpenNLP models need to be downloaded into the directory 'models' from [here](http://opennlp.sourceforge.net/models-1.5/)
  
  As a result the following folders should be in in /resources:
  As obtained [from git](https://github.com/bgalitsky/relevance-based-on-parse-trees/tree/master/src/test/resources):
  <li>/new_vn (VerbNet)</li>
  <li>/maps (some lookup files)</li>
  <li>/external_rst (examples of import of rhetoric parses from other systems)</li>
  <li>/fca (Formal Concept Analysis learning)</li>
  <li>/taxonomies (for search support, taxonomies are auto-mined from the web)
  
  Manual downloading is required:
  /new_vn
  /w2v (where word2vector model needs to be downloaded)
  
 
## Engines and Systems of OpenNLP.Similarity

### Main relevance assessment function
It takes two texts and returns the cardinality of a maximum common subgraph representations of these texts. This measure is supposed to be much more accurate than keyword statistics, compositional semantic models word2vec because linguistic structure is taken into account, not just co-occurrences of keywords. 
  [Matching class](https://github.com/bgalitsky/relevance-based-on-parse-trees/blob/master/src/main/java/opennlp/tools/parse_thicket/matching/Matcher.java) in [matching package] (https://github.com/bgalitsky/relevance-based-on-parse-trees/tree/master/src/main/java/opennlp/tools/parse_thicket/matching) has 
List<List<ParseTreeChunk>> assessRelevance(String para1, String para2)
function which returns the list of common phrases between these paragraphs.

To avoid re-parsing the same strings and improve the speed, use
List<List<ParseTreeChunk>> assessRelevanceCache(String para1, String para2)

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
 It takes a topic, build a taxonomy for it, forms a table of content. It then  mines the web for documents for each table of content item, finds relevant sentences and paragraphs and merges them into a document [package](https://github.com/bgalitsky/relevance-based-on-parse-trees/tree/master/src/main/java/opennlp/tools/similarity/apps). We attempt to reproduce how humans cut-and-paste content from the web while writing on a topic.
  Content generation has a [demo](http://37.46.135.20/)  and to run it from IDE start [here](https://github.com/bgalitsky/relevance-based-on-parse-trees/blob/master/src/main/java/opennlp/tools/similarity/apps/ContentGeneratorRunner.java). Examples of written documents are [here](http://37.46.135.20/wrt_latest/).
 
<li>Document builder with TOC, Sections, Figures& Captions and reference section</li>
<li>Review builder via taking a number of existing reviews and cross-breeding them, to write them so that they look "original"</li>

### Text detector / classifier
The [classifier code](https://github.com/bgalitsky/relevance-based-on-parse-trees/blob/master/src/main/java/opennlp/tools/parse_thicket/kernel_interface/TreeKernelBasedClassifierMultiplePara.java) is the same but the [model files](https://github.com/bgalitsky/relevance-based-on-parse-trees/tree/master/src/test/resources/tree_kernel/TRAINING) vary for the applications below:
<li>detect security leaks
<li>detect argumentation
<li>detect low cohesiveness in text
<li>detect authors’ doubt and low confidence
<li>detect fake review

### General-purpose [deterministic inductive learner](https://github.com/bgalitsky/relevance-based-on-parse-trees/tree/master/src/main/java/opennlp/tools/jsmlearning) implements JS Mills method of induction and abduction (deduction is also partially implemented).

 Inductive learning implemented as a base for syntactic tree-based learning is similar to the family of approaches such as Explanation-based learning and Inductive Logic Programming

### Filtering results for Speech Recognition based on semantic meaningfulness.
It takes results from a speech-to-text system and subjects them to [filtering]
(https://github.com/bgalitsky/relevance-based-on-parse-trees/blob/master/src/main/java/opennlp/tools/similarity/apps/SpeechRecognitionResultsProcessor.java)
## Related Research
Here's the link to the book on [question-answering](https://www.amazon.com/Natural-Language-Question-Answering-system/dp/0868039799/ref=sr_1_10?ie=UTF8&qid=1478871097&sr=8-10&keywords=galitsky)

and [research papers](https://scholar.google.com/citations?hl=ru&user=kR_M3HIAAAAJ).

Also the recent [book related to reasoning and linguistics in humans & machines](https://www.amazon.com/Computational-Autism-Human-Computer-Interaction-Galitsky/dp/3319399713)