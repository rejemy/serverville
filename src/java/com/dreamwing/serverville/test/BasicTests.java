package com.dreamwing.serverville.test;

import org.junit.Assert;

import com.dreamwing.serverville.util.SVID;
import com.dreamwing.serverville.util.SVID.SVIDInfo;

public class BasicTests {

	@Test(order=1)
	public void TestSVIDGenerator()
	{
		String id = SVID.makeSVID();
		Assert.assertNotNull(id);
		Assert.assertTrue(id.compareTo("..K4Kty5..0wQV") > 0);
		String id2 = SVID.makeSVID();
		Assert.assertTrue("SVIDs not increasing? "+id+" "+id2, id.compareTo(id2) < 0);
		
		SVIDInfo info = SVID.getSVIDInfo(id);
		Assert.assertNotNull(info);
	}
	
	
}
