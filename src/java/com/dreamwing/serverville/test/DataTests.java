package com.dreamwing.serverville.test;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;

import com.dreamwing.serverville.data.KeyData;
import com.dreamwing.serverville.data.KeyDataItem;
import com.dreamwing.serverville.data.KeyDataTypes;
import com.dreamwing.serverville.db.KeyDataManager;
import com.dreamwing.serverville.util.SVID;

public class DataTests {


	@Test(order=1)
	public void LoadNonexistantItem() throws Exception
	{
		KeyDataItem result = KeyDataManager.loadKey(SVID.makeSVID(), "nobody");
		Assert.assertNull(result);
	}
	
	private static String TestItemID = SVID.makeSVID();
	private static byte[] TestItemBytes;
	
	@Test(order=2)
	public void SaveItem() throws Exception
	{
		TestItemBytes = new byte[10];

		for(int i=0; i<TestItemBytes.length; i++)
			TestItemBytes[i] = 0;
		
		KeyDataManager.saveKey(TestItemID, "testkey", TestItemBytes, KeyDataTypes.BYTES);
	}
	
	@Test(order=3)
	public void UpdateItem() throws Exception
	{
		for(int i=0; i<TestItemBytes.length; i++)
			TestItemBytes[i] = 1;
		
		KeyDataManager.saveKey(TestItemID, "testkey", TestItemBytes, KeyDataTypes.BYTES);
	}
	
	@Test(order=4)
	public void LoadItem() throws Exception
	{
		KeyDataItem result = KeyDataManager.loadKey(TestItemID, "testkey");
		Assert.assertNotNull(result);
		Assert.assertNotNull(result.data);
		Assert.assertEquals("testkey", result.key);
		Assert.assertArrayEquals(TestItemBytes, result.data);
		long now = System.currentTimeMillis();
		Assert.assertTrue(now >= result.created);
		Assert.assertTrue(now >= result.modified);
	}
	
	@Test(order=5)
	public void DeleteItem() throws Exception
	{
		KeyDataManager.deleteKey(TestItemID, "testkey");
		KeyDataItem result = KeyDataManager.loadKey(TestItemID, "testkey");
		Assert.assertNull(result);
		result = KeyDataManager.loadKey(TestItemID, "testkey", true);
		Assert.assertNotNull(result);
		KeyDataManager.purgeDeletedKeysFor(TestItemID);
	}
	
	private static KeyDataItem BatchData[] = new KeyDataItem[]
		{
			new KeyDataItem("BatchTestKey1", new byte[]{0,0,0}, KeyDataTypes.BYTES),
			new KeyDataItem("BatchTestKey2", new byte[]{0,-1,1}, KeyDataTypes.BYTES),
			new KeyDataItem("BatchTestKey3", new byte[]{0,-2,2}, KeyDataTypes.BYTES),
			new KeyDataItem("BatchTestKey4", new byte[]{0,-3,3}, KeyDataTypes.BYTES),
			new KeyDataItem("BatchTestKey5", new byte[]{0,-4,4}, KeyDataTypes.BYTES)
		};
	
	private static long Batch1SaveTime;
	
	@Test(order=6)
	public void BatchSave() throws Exception
	{
		Batch1SaveTime = KeyDataManager.saveKeys(TestItemID, Arrays.asList(BatchData));
	}
	
	@Test(order=7)
	public void LoadAllKeys() throws Exception
	{
		List<KeyDataItem> results = KeyDataManager.loadAllKeys(TestItemID);
		Assert.assertNotNull(results);
		Assert.assertEquals(BatchData.length, results.size());
		
		long now = System.currentTimeMillis();
		
		for(int i=0;i<BatchData.length;i++)
		{
			KeyDataItem result = results.get(i);
			KeyDataItem expected = BatchData[i];
			
			Assert.assertEquals(expected.key, result.key);
			Assert.assertArrayEquals(expected.data, result.data);
			
			Assert.assertTrue(now >= result.created);
			Assert.assertTrue(now >= result.modified);
		}
	}
	
	private static KeyDataItem Batch2Data[] = new KeyDataItem[]
		{
			new KeyDataItem("BatchTestKey6", new byte[]{0,-6,6}, KeyDataTypes.BYTES),
			new KeyDataItem("BatchTestKey7", new byte[]{0,-7,7}, KeyDataTypes.BYTES),
			new KeyDataItem("BatchTestKey8", new byte[]{0,-8,8}, KeyDataTypes.BYTES)
		};
	
	@Test(order=8)
	public void LoadAllKeysSince() throws Exception
	{
		long batch2SaveTime = KeyDataManager.saveKeys(TestItemID, Arrays.asList(Batch2Data));
		
		List<KeyDataItem> results = KeyDataManager.loadAllKeysSince(TestItemID, Batch1SaveTime);
		Assert.assertNotNull(results);
		Assert.assertEquals(Batch2Data.length, results.size());
		
		long now = System.currentTimeMillis();
		
		for(int i=0;i<Batch2Data.length;i++)
		{
			KeyDataItem result = results.get(i);
			KeyDataItem expected = Batch2Data[i];
			
			Assert.assertEquals(expected.key, result.key);
			Assert.assertArrayEquals(expected.data, result.data);
			
			Assert.assertTrue(now >= result.created);
			Assert.assertTrue(now >= result.modified);
		}
		
		List<KeyDataItem> batch2results = KeyDataManager.loadAllKeysSince(TestItemID, batch2SaveTime);
		Assert.assertNull(batch2results);
	}
	
	@Test(order=9)
	public void DeleteAllKeys() throws Exception
	{
		List<KeyDataItem> preDeleteResults = KeyDataManager.loadAllKeys(TestItemID);
		
		KeyDataManager.deleteAllKeys(TestItemID);
		List<KeyDataItem> results = KeyDataManager.loadAllKeys(TestItemID);
		Assert.assertNull(results);
		
		results = KeyDataManager.loadAllKeys(TestItemID, true);
		Assert.assertNotNull(results);
		Assert.assertEquals(preDeleteResults.size(), results.size());
	}
	
	@Test(order=10)
	public void PurgeDeleted() throws Exception
	{
		KeyDataManager.purgeDeletedKeysFor(TestItemID);
		
		List<KeyDataItem> results = KeyDataManager.loadAllKeys(TestItemID, true);
		Assert.assertNull(results);
	}
	
	@Test(order=11)
	public void KeyDataCreation() throws Exception
	{
		// Start a new ID for a new series of tests
		TestItemID = SVID.makeSVID();
		
		KeyData randomData = new KeyData(TestItemID);
		randomData.set("str", "Woohoo!");
		randomData.set("int", 100);
		randomData.set("float", 1.0f);
		randomData.save();
		
		KeyData data2 = new KeyData(TestItemID);
		data2.loadAll();
		
		Assert.assertEquals(randomData.getAsString("str"), data2.getAsString("str"));
		Assert.assertEquals(randomData.getAsInt("int"), data2.getAsInt("int"));
		Assert.assertEquals(randomData.getAsFloat("float"), data2.getAsFloat("float"));
	}
	
	@Test(order=12)
	public void KeyDataRefresh() throws Exception
	{
		KeyData data = new KeyData(TestItemID);
		data.loadAll();
		
		KeyDataManager.saveKeyValue(TestItemID, "str", "yo");
		KeyDataManager.saveKeyValue(TestItemID, "testkey", "new");
		KeyDataManager.deleteKey(TestItemID, "float");
		
		data.refresh();
		
		Assert.assertEquals("yo", data.getAsString("str"));
		Assert.assertEquals("new", data.getAsString("testkey"));
		Assert.assertNull(data.getKeyData("float"));
	}
	
	@Test(order=13)
	public void KeyDataDelete() throws Exception
	{
		KeyData data = new KeyData(TestItemID);
		data.loadAll();
		
		data.deleteAllKeys();
		data.save();
		
		KeyDataManager.purgeDeletedKeysFor(TestItemID);
		
	}
	
}
