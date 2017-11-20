package com.dreamwing.serverville.ext.braintree;

import java.sql.SQLException;
import java.util.Currency;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.ClientTokenRequest;
import com.braintreegateway.Environment;
import com.braintreegateway.Result;
import com.braintreegateway.Transaction;
import com.braintreegateway.TransactionRequest;
import com.dreamwing.serverville.ServervilleMain;
import com.dreamwing.serverville.data.Product;
import com.dreamwing.serverville.data.Purchase;
import com.dreamwing.serverville.data.ServervilleUser;
import com.dreamwing.serverville.net.ApiErrors;
import com.dreamwing.serverville.net.JsonApiException;
import com.dreamwing.serverville.purchases.PurchaseManager;
import com.dreamwing.serverville.util.CurrencyUtil;

public class BraintreeInterface
{
private static final Logger l = LogManager.getLogger(BraintreeInterface.class);
	
	private static BraintreeGateway Braintree;

	public static void init() throws Exception
	{
		String merchantId = ServervilleMain.ServerProperties.getProperty("braintree_merchant_id");
		if(merchantId == null || merchantId.length() == 0)
			return;
		
		String publicKey = ServervilleMain.ServerProperties.getProperty("braintree_public_key");
		if(publicKey == null || publicKey.length() == 0)
			return;
		
		String privateKey = ServervilleMain.ServerProperties.getProperty("braintree_private_key");
		if(privateKey == null || privateKey.length() == 0)
			return;
		
		Environment env = null;
		String environmentStr = ServervilleMain.ServerProperties.getProperty("braintree_environment").toLowerCase();
		if(environmentStr.equals("production"))
			env = Environment.PRODUCTION;
		else if(environmentStr.equals("sandbox"))
			env = Environment.SANDBOX;
		else
			throw new Exception("Unknown Braintree environment: "+environmentStr);
		
		Braintree = new BraintreeGateway(env, merchantId, publicKey, privateKey);
		
		l.info("Braintree interface initialized");
	}
	
	public static String getClientToken(ServervilleUser user, int version)
	{
		ClientTokenRequest clientTokenRequest = new ClientTokenRequest().customerId(user.getId()).version(version);
		return Braintree.clientToken().generate(clientTokenRequest);
	}
	
	public static void makePurchase(ServervilleUser user, Product product, Currency currency, String nonce) throws SQLException, JsonApiException
	{
		int price = product.Price.get(currency.getCurrencyCode());
		
		Purchase purchase = PurchaseManager.startUserPurchase(user, product, currency.getCurrencyCode(), "Braintree");
		
		TransactionRequest request = new TransactionRequest()
			.amount(CurrencyUtil.getPriceAsDecimal(currency, price))
			.paymentMethodNonce(nonce)
			.merchantAccountId(currency.getCurrencyCode())
			.options()
				.submitForSettlement(true)
				.done();
		
		Result<Transaction> result = Braintree.transaction().sale(request);
		if(result.isSuccess())
		{
			String transactionId = result.getTransaction().getId();
			PurchaseManager.completePurchase(user, purchase, transactionId);
		}
		else
		{
			String message = result.getMessage();
			PurchaseManager.failPurchase(purchase, message);
			l.error("Error charging Braintree", message);
			throw new JsonApiException(ApiErrors.CHARGE_ERROR, message);
		}
		
	}
	
}
