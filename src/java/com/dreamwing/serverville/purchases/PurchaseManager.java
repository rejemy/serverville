package com.dreamwing.serverville.purchases;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.CurrencyInfoManager;
import com.dreamwing.serverville.ProductManager;
import com.dreamwing.serverville.data.CurrencyInfo;
import com.dreamwing.serverville.data.KeyDataItem;
import com.dreamwing.serverville.data.Product;
import com.dreamwing.serverville.data.Purchase;
import com.dreamwing.serverville.data.ServervilleUser;
import com.dreamwing.serverville.db.KeyDataManager;
import com.dreamwing.serverville.net.JsonApiException;
import com.dreamwing.serverville.scripting.ScriptEngineContext;
import com.dreamwing.serverville.scripting.ScriptManager;
import com.dreamwing.serverville.serialize.JsonDataDecoder;
import com.dreamwing.serverville.util.SVID;

public class PurchaseManager
{
	private static final Logger l = LogManager.getLogger(PurchaseManager.class);
	
	public static Purchase compPurchase(ServervilleUser user, Product product) throws SQLException
	{
		Purchase purchase = startUserPurchase(user, product, null, "comp");
		completePurchase(user, purchase, null);
		return purchase;
	}
	
	public static Purchase startUserPurchase(ServervilleUser user, Product product, String currency, String method) throws SQLException
	{
		Integer price = null;
		if(currency != null)
			price = product.Price.get(currency);
		
		Purchase purchase = new Purchase();
		purchase.PuchaseId = SVID.makeSVID();
		purchase.UserId = user.getId();
		purchase.Created = new Date();
		purchase.ProductId = product.ProductId;
		purchase.Method = method;
		purchase.Currency = currency;
		purchase.Price = price;
		purchase.Success = false;
		
		purchase.create();
		
		return purchase;
	}
	
	public static void completePurchase(ServervilleUser user, Purchase purchase, String transactionId) throws SQLException
	{
		purchase.Success = true;
		purchase.TransactionId = transactionId;
		
		Product product = ProductManager.getProduct(purchase.ProductId);
		
		try
		{
			grantProduct(user, product);
		}
		catch(Exception e)
		{
			l.error("Error granding product", e);
			purchase.Error = e.getMessage();
			purchase.Success = false;
			
		}

		purchase.update();
	}
	
	public static void failPurchase(Purchase purchase, String error) throws SQLException
	{
		purchase.Error = error;
		
		purchase.update();
	}
	
	private static void grantProduct(ServervilleUser user, Product product) throws SQLException, JsonApiException
	{
		if(product.Currencies != null)
		{
			for(Map.Entry<String,Integer> currencyItr: product.Currencies.entrySet())
			{
				CurrencyInfo currency = CurrencyInfoManager.getCurrencyInfo(currencyItr.getKey());
				int amount = currencyItr.getValue();
				CurrencyInfoManager.changeCurrencyBalance(user, currency, amount, "Purchase: "+product.ProductId);
			}
		}
		
		if(product.KeyData != null)
		{
			List<KeyDataItem> itemList = new ArrayList<KeyDataItem>(product.KeyData.size());
			
			for(Map.Entry<String,Object> valueItr: product.KeyData.entrySet())
			{
				KeyDataItem item = JsonDataDecoder.MakeKeyDataFromJson(valueItr.getKey(), null, valueItr.getValue());
				itemList.add(item);
			}
			
			KeyDataManager.saveKeys(user.getId(), itemList);
		}

		if(product.Script != null)
		{
			ScriptEngineContext engine = ScriptManager.getEngine();
			try
			{
				engine.invokeCallbackHandler(product.Script, user.getId(), product.ProductId);
			} catch (Exception e) {
				l.error("Error executing onListenToChannel handler: ", e);
			}
			finally
			{
				ScriptManager.returnEngine(engine);
			}
		}
		
	}
}
