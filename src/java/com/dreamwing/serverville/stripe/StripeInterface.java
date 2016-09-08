package com.dreamwing.serverville.stripe;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dreamwing.serverville.ServervilleMain;
import com.dreamwing.serverville.data.Product;
import com.dreamwing.serverville.data.Purchase;
import com.dreamwing.serverville.data.ServervilleUser;
import com.dreamwing.serverville.net.ApiErrors;
import com.dreamwing.serverville.net.JsonApiException;
import com.dreamwing.serverville.purchases.PurchaseManager;

import com.stripe.Stripe;
import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Charge;

public class StripeInterface
{
	private static final Logger l = LogManager.getLogger(StripeInterface.class);
	
	public static void init()
	{
		String apiKey = ServervilleMain.ServerProperties.getProperty("stripe_api_key");
		if(apiKey == null || apiKey.length() == 0)
			return;
		
		Stripe.apiKey = apiKey;
	}
	
	public static void makePurchase(ServervilleUser user, Product product, String currency, String stripeToken) throws JsonApiException, SQLException
	{
		
		int price = product.Price.get(currency);
		
		Purchase purchase = PurchaseManager.startUserPurchase(user, product, currency, "Stripe");
		
		Map<String, Object> chargeParams = new HashMap<String, Object>();
		chargeParams.put("amount", price);
		chargeParams.put("currency", currency);
		chargeParams.put("source", stripeToken);
		chargeParams.put("description", product.ProductId);
		
		Map<String, String> userMetadata = new HashMap<String, String>();
		userMetadata.put("user_id", user.getId());
		userMetadata.put("purchase_id", purchase.PuchaseId);
		userMetadata.put("product_id", product.ProductId);
		chargeParams.put("metadata", userMetadata);
		
		Charge charge = null;
		
		try {
			charge = Charge.create(chargeParams);
		} catch (AuthenticationException | InvalidRequestException | APIConnectionException | CardException | APIException e)
		{
			PurchaseManager.failPurchase(purchase, e.getMessage());
			l.error("Error charging stripe", e);
			throw new JsonApiException(ApiErrors.CHARGE_ERROR, e.getMessage());
		}
		
		String chargeId = charge.getId();
		
		PurchaseManager.completePurchase(user, purchase, chargeId);
		
	}
}
