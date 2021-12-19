package opennlp.tools.chatbot.text_searcher;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.spans.SpanQuery;

import org.apache.lucene.search.*;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.spans.*;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

// see explanations in https://lucidworks.com/2009/07/18/the-spanquery/

public class SpanQueryBuilder {
        private Query query=null;
        private SpanQuery squery=null;
        private SpanQuery nquery=null;
        private static int MAX_CLAUSE_COUNT = 50000;
        private static int MAX_LOG_LENGTH = 10000; //?

        private static char[] alpha = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I',
                'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'X',
                'Y', 'Z', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0'
        };
        static {
            BooleanQuery.setMaxClauseCount(MAX_CLAUSE_COUNT);
        }
         private ArrayList m_negQueriesAccum = new ArrayList();  //negative component
        private String messageFromSpanSearcher=null;
        private boolean mDeviceConstraint=true;

    public SpanQueryBuilder( String searchString, IndexSearcher is) throws Exception
    {
            String searchStringLower= new String(searchString.toUpperCase());  // keywords AND, OR and NOT need to be uppercase
            QueryParser qp = new QueryParser("phrase", new StandardAnalyzer());
            qp.setDefaultOperator(QueryParser.Operator.AND);
            query=qp.parse(searchStringLower);
                 System.out.println("parsed qry: "+query.toString());
            squery= transformQuery(query,is); //build span query
            if (squery==null) //query formation failed - no starts are returned
            {
                messageFromSpanSearcher="unable to build a span query from your query";
                System.out.println("There was a problem forming query");
                return;
            }
                System.out.println("transformed qry: "+squery.toString());
           
            nquery = processLongNegQuery( m_negQueriesAccum);
            if (squery==null) //query formation failed - no starts are returned
            {
                messageFromSpanSearcher="unable to understand your query";
                System.out.println("There was a problem forming query");
                return;
            }
    }
    // forms Lucene span query from list of terms in a multiword (NER) extracted linguistically
    // if single keyword, or more than 3, return null
    
    public static SpanNearQuery formSpanQueryFromTokens(List<String> tokens, String field){
    	SpanNearQuery spanNear = null;
    	
    	if (tokens.size()==2)
    		spanNear = new SpanNearQuery(new SpanQuery[] {
    			  new SpanTermQuery(new Term(field, tokens.get(0))),
    			  new SpanTermQuery(new Term(field, tokens.get(1)))},
    			  3,
    			  true);
    	if (tokens.size()==3)
    		spanNear = new SpanNearQuery(new SpanQuery[] {
    			  new SpanTermQuery(new Term(field, tokens.get(0))),
    			  new SpanTermQuery(new Term(field, tokens.get(1))),
    			  new SpanTermQuery(new Term(field, tokens.get(2)))
    			  },
    			  3,
    			  true);
    	if (tokens.size()==4)
    		spanNear = new SpanNearQuery(new SpanQuery[] {
    			  new SpanTermQuery(new Term(field, tokens.get(0))),
    			  new SpanTermQuery(new Term(field, tokens.get(1))),
    			  new SpanTermQuery(new Term(field, tokens.get(2))),
    			  new SpanTermQuery(new Term(field, tokens.get(3))),
    			  },
    			  3,
    			  true);
    	
    	return spanNear;
    }
    
        /** get method for the class. gets boolean query
         *  @return the parsed boolean query (not a Span query). this is an intermediate result of span query formation
         * */
        public Query getOriginalQuery(){
            return query;
        }
         /** get method for the class. gets span query
         *  @return the Span query, or a postive components (excluding negation terms)
         * */
        public SpanQuery getSpanQuery(){
            return squery;
        }

        /** get method for the class. gets negative span query
         *  @return the negative component of a Span query (negative terms extracted from a query)
         */
        public SpanQuery getNegSpanQuery(){
            return nquery;
        }

        public String getMessageFromSpanSearcher() {
             return messageFromSpanSearcher;
        }

        public boolean getDeviceConstraint() {
            return mDeviceConstraint;
        }
        private  SpanQuery transformQuery(Query query, IndexSearcher searcher) {
            try {
                if(( query == null) ) return null; // no filter
                //if (query instanceof TermQuery) return null
                Query rewrittenQuery = searcher.rewrite(query);
                if (rewrittenQuery ==null)
                    return null;
                Query cleanedQuery = rewrittenQuery;
                if (cleanedQuery==null)
                    return null;
                SpanQuery sq = queryToSpanQuery(cleanedQuery);
                return sq;
         } catch(Exception e) {
                //System.out.println("There was a problem transforming query");
                return null;
            }
        }

        /* remove any empty boolean clauses
         * Empty boolean clauses result from rewriting a multiterm query.
         * This function returns null if there are no terms left in the query
         * otherwise returns a smaller set.
         * If a required clause is empty, it sets missingRequiredClause to true 
        private  Query removeEmptyClauses(Query query) {
            if(query instanceof BooleanQuery) {
                Collection<Query> clauses = ((BooleanQuery) query).getClauses(Occur.SHOULD);
                if(clauses.length == 0) return null;
                else {
                    for(int i = 0; i < clauses.length; i++) {
                        clauses[i].setQuery(removeEmptyClauses(clauses[i].getQuery()));
                    }
                    return makeNewQuery(clauses);
                }
            } else return query;
        }

        private  Query makeNewQuery(BooleanClause[] clauses) {
            if(clauses.length == 0) return null;
            for(int i = 0; i < clauses.length; i++) {
                if((clauses[i].getQuery() == null) && (clauses[i].isRequired())){
                    return null;
                }
            }
            BooleanQuery bq = new BooleanQuery(true, 1, clauses);
            for(int i = 0; i < clauses.length; i++) {
                if(clauses[i].getQuery() != null) bq.add(clauses[i]);
            }
            if(bq.getClauses().length == 0) return null;
            else return bq;
        }
*/

        /** used in recursive function to form a span query
         *  return a span query if possible. Otherwise return null
         */
        private  SpanQuery queryToSpanQuery(Query query) {
            if(query instanceof BooleanQuery) {
                return booleanQueryToSpanQuery((BooleanQuery) query);
            } else if(query instanceof PhraseQuery) {
                return phraseQueryToSpanQuery((PhraseQuery) query);
            } else if(query instanceof TermQuery) {
                return termQueryToSpanQuery((TermQuery) query) ;
            } else
                return null;
        }

        /** If there is any prohibited clause in a boolean query return true, otherwise
         * return false */
         /**
         * Transform a BooleanQuery into a span query
         */
    private  SpanQuery booleanQueryToSpanQuery(BooleanQuery query) {
            List<BooleanClause> clauses = ((BooleanQuery) query).clauses();
            if(clauses.size() == 1)
                return queryToSpanQuery(clauses.get(0).getQuery());

            Vector requiredClauses = new Vector();  // to be in INCLUDED if NOT exists
            Vector prohibitedClauses = new Vector();// to be in EXCLUDED if NOT exists
            Vector otherClauses = new Vector();

            for(int i = 0; i < clauses.size(); i++) {
                if(clauses.get(i).isRequired())
                    requiredClauses.add(clauses.get(i));
                else if(clauses.get(i).isProhibited())
                    prohibitedClauses.add(clauses.get(i));
                else
                    otherClauses.add(clauses.get(i));
            }
            SpanQuery  spPos=null; //SpanQuery  spNeg=null;
            int posSize=requiredClauses.size(); int negSize=prohibitedClauses.size();
            if (posSize+negSize >= 1) {
                SpanQuery[] posQueries = new SpanQuery[requiredClauses.size()];
                //int prohClSize=0;
                //if (prohibitedClauses.size()>0) prohClSize=1;
                //SpanQuery[] negQueries = new SpanQuery[prohClSize];

                if (posSize>0){
                    for(int i = 0; i < requiredClauses.size(); i++) {
                        posQueries[i] =
                            queryToSpanQuery(((BooleanClause)  requiredClauses.elementAt(i)).getQuery());
                    }
                    spPos= new SpanNearQuery(posQueries, (int)(MAX_LOG_LENGTH/2), false); //any order
                }
                if (negSize>0){
                  //  negQueries[0] =
                  //          queryToSpanQuery(((BooleanClause) prohibitedClauses.elementAt(0)).getQuery());
                    for(int i = 0; i < prohibitedClauses.size(); i++) {
                        m_negQueriesAccum.add(
                            queryToSpanQuery(((BooleanClause) prohibitedClauses.elementAt(i)).getQuery()));
                    }
                    //spNeg= new SpanNearQuery(negQueries, (int)(MAX_LOG_LENGTH/2), false);
                }
               // if (posSize*negSize>0) //both present
               //   return new SpanNotQuery(spPos, spNeg);
               // else { if (posSize>0) return spPos; else return spNeg;}
                  return spPos;
            }
            else if (otherClauses.size() > 0) { // treat it as an OR query
                SpanQuery[] orQueries = new SpanQuery[otherClauses.size()];
                for(int i = 0; i < otherClauses.size(); i++) {
                    orQueries[i] = queryToSpanQuery(((BooleanClause) otherClauses.elementAt(i)).getQuery());
                }
                return new SpanOrQuery(orQueries);
            } else return null;
        }

    private SpanQuery processLongNegQuery(ArrayList negQueriesAccum){
            if (negQueriesAccum.isEmpty()) return null;
            SpanQuery spPos_fromNegQry = null;
            if (negQueriesAccum.get(0) instanceof SpanOrQuery)
                spPos_fromNegQry = (SpanOrQuery)negQueriesAccum.get(0);
            else {
            SpanTermQuery[] stq= (SpanTermQuery[]) negQueriesAccum.toArray(new SpanTermQuery[1]);
            spPos_fromNegQry= new SpanOrQuery(stq);
            }
            if (spPos_fromNegQry==null){
                messageFromSpanSearcher="unable to understand your query (negative component)";
                System.out.println("There was a problem forming query (negative component)");
                return null;
            }

            return spPos_fromNegQry;
        }

    private  SpanQuery phraseQueryToSpanQuery(PhraseQuery query) {
            Term[] queryTerms = query.getTerms();
            SpanTermQuery[] stq = new SpanTermQuery[queryTerms.length];
            for(int i = 0; i < queryTerms.length; i++) {
                stq[i] = new SpanTermQuery(queryTerms[i]);
            }
            return new SpanNearQuery(stq, (int)(MAX_LOG_LENGTH/*/2*/), true);  //order maters for phrase query
        }

        private  SpanQuery termQueryToSpanQuery(TermQuery query) {
            return new SpanTermQuery(query.getTerm());
        }

        public String checkAndExpandWildcard(String exp){
            //break the exp into each terms
            StringTokenizer st = new StringTokenizer(exp, " ");
            String token = "";
            String newExp = "";
            while (st.hasMoreTokens()){
                token = st.nextToken();
                if (token.startsWith("*")){
                    token = expandWildcard(token);
                }
                newExp += token + " ";
            }
            return newExp.trim();
        }

        private  SpanQuery addDeviceQuery(ArrayList deviceList, SpanQuery sq){
            if (deviceList==null || deviceList.size() == 0 ||
                    (deviceList.size() == 1 && ((String)deviceList.get(0)).equals("0"))){
                mDeviceConstraint=false;
                return sq; //empty, null or "0" - list of devices (ALL)
            }

            SpanQuery[] devQueries = new SpanQuery[deviceList.size()];
            for(int i=0; i<deviceList.size(); i++){
                devQueries[i]= new SpanTermQuery(new Term("contents", (String) deviceList.get(i)));
            }
            SpanQuery[] dqs= new SpanQuery[2];
            dqs[0]=new SpanOrQuery(devQueries);
            dqs[1]=sq;
            return new SpanNearQuery(dqs, MAX_LOG_LENGTH, true);  //order does matter : device should be in front of other terms
        }

        public String expandWildcard(String exp){

            String expandedExp  = "";

            for (int i = 0; i < alpha.length; i++){
                expandedExp += alpha[i] + exp + " OR ";
            }
            int index = expandedExp.lastIndexOf("OR");
            expandedExp = expandedExp.substring(0, index);

            return expandedExp;
        }
}

