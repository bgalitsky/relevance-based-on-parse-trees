package opennlp.tools.parse_thicket.matching;

import java.util.List;

import opennlp.tools.textsimilarity.ParseTreeChunk;
import junit.framework.TestCase;

public class PTMatcherTest extends TestCase {
	Matcher m = new Matcher();

	public void testMatchTwoParaTest(){
		List<List<ParseTreeChunk>> res = m.assessRelevance("Iran refuses to accept the UN proposal to end its dispute over its work on nuclear weapons."+
				"UN nuclear watchdog passes a resolution condemning Iran for developing its second uranium enrichment site in secret. " +
				"A recent IAEA report presented diagrams that suggested Iran was secretly working on nuclear weapons. " +
				"Iran envoy says its nuclear development is for peaceful purpose, and the material evidence against it has been fabricated by the US. "

			, "Iran refuses the UN offer to end a conflict over its nuclear weapons."+
					"UN passes a resolution prohibiting Iran from developing its uranium enrichment site. " +
					"A recent UN report presented charts saying Iran was working on nuclear weapons. " +
				"Iran envoy to UN states its nuclear development is for peaceful purpose, and the evidence against its claim is fabricated by the US. ");
		System.out.print(res);
		assertTrue(res!=null);
		assertTrue(res.size()>0);
	}
}


