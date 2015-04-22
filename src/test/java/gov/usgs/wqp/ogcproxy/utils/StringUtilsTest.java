package gov.usgs.wqp.ogcproxy.utils;

import static org.junit.Assert.*;
import static gov.usgs.wqp.ogcproxy.utils.StringUtils.*;

import org.junit.Test;

public class StringUtilsTest {


	static enum A {
		Aa, bb, Cc, FALLBACK;
	}

	
	@Test
	public void testSentenceCase_getStringFromType_simple() {
		assertEquals("Aa", getStringFromType(A.Aa, A.FALLBACK));
	}
		
	@Test
	public void testSentenceCase_getStringFromType_null() {
		assertEquals(A.FALLBACK.toString(), getStringFromType(null, A.FALLBACK));
	}
	
	@Test
	public void testSentenceCase_getStringFromType_lower() {
		assertEquals("bb", getStringFromType(A.bb, A.FALLBACK));
	}
	
	@Test
	public void testSentenceCase_getStringFromType_FALLBACK() {
		assertEquals("FALLBACK", getStringFromType(A.FALLBACK, A.FALLBACK));
	}
		
		
	@Test
	public void testSentenceCase_getSentenceCaseStringFromType_simple() {
		assertEquals("Aa", getSentenceCaseStringFromType(A.Aa, A.FALLBACK));
	}
		
	@Test
	public void testSentenceCase_getSentenceCaseStringFromType_lower() {
		assertEquals("Bb", getSentenceCaseStringFromType(A.bb, A.FALLBACK));
	}
		
	@Test
	public void testSentenceCase_getSentenceCaseStringFromType_null() {
		assertEquals(A.FALLBACK.toString(), getSentenceCaseStringFromType(null, A.FALLBACK));
	}
		
	@Test
	public void testSentenceCase_getSentenceCaseStringFromType_FALLBACK() {
		assertEquals("FALLBACK", getSentenceCaseStringFromType(A.FALLBACK, A.FALLBACK));
	}
		
	
	
	
	@Test
	public void testSentenceCase_getTypeFromString_simple() {
		assertEquals(A.Aa, getTypeFromString("Aa", A.FALLBACK));
	}
	
	@Test
	public void testSentenceCase_getTypeFromString_fallback() {
		assertEquals(A.FALLBACK, getTypeFromString("unknown", A.FALLBACK));
		assertEquals(A.FALLBACK, getTypeFromString("aa", A.FALLBACK));
	}
	
	@Test
	public void testSentenceCase_getTypeFromString_null() {
		assertEquals(A.FALLBACK, getTypeFromString(null, A.FALLBACK));
	}
	
	@Test
	public void testSentenceCase_getTypeFromString_empty() {
		assertEquals(A.FALLBACK, getTypeFromString("", A.FALLBACK));
	}
	
	
	
	
	@Test
	public void testSentenceCase_simple() {
		assertEquals("Aa", sentenceCase("aa"));
	}

	@Test
	public void testSentenceCase_null() {
		assertEquals(null, sentenceCase(null));
	}

	@Test
	public void testSentenceCase_empty() {
		assertEquals("", sentenceCase(""));
	}

	@Test
	public void testSentenceCase_oneChar() {
		assertEquals("Aa", sentenceCase("aa"));
	}
}
