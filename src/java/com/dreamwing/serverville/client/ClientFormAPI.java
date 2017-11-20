package com.dreamwing.serverville.client;

import java.net.URL;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.Currency;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.dreamwing.serverville.ProductManager;
import com.dreamwing.serverville.data.Product;
import com.dreamwing.serverville.data.ServervilleUser;
import com.dreamwing.serverville.data.UserSession;
import com.dreamwing.serverville.ext.stripe.StripeInterface;
import com.dreamwing.serverville.net.ApiErrors;
import com.dreamwing.serverville.net.HttpHandlerOptions;
import com.dreamwing.serverville.net.HttpHelpers;
import com.dreamwing.serverville.net.HttpRequestInfo;
import com.dreamwing.serverville.net.JsonApiException;
import com.dreamwing.serverville.util.CurrencyUtil;

import io.netty.channel.ChannelFuture;

public class ClientFormAPI
{
	private static final Logger l = LogManager.getLogger(ClientFormAPI.class);
	
	@HttpHandlerOptions(method=HttpHandlerOptions.Method.POST, auth=false)
	public static ChannelFuture stripeCheckout(HttpRequestInfo req) throws JsonApiException, SQLException
	{
		String stripeToken = req.getOneBody("stripeToken");
		String sessionId = req.getOneBody("session_id");
		String productId = req.getOneBody("product_id");

		String successUrlStr = req.getOneBody("success_redirect_url", null);
		String failUrlStr = req.getOneBody("fail_redirect_url", null);
		
		UserSession session = UserSession.findById(sessionId);
		if(session == null)
		{
			throw new JsonApiException(ApiErrors.BAD_AUTH);
		}
		
		ServervilleUser user = ServervilleUser.findById(session.UserId);
		if(user == null)
		{
			throw new JsonApiException(ApiErrors.BAD_AUTH);
		}
		
		URL successUrl = null;
		URL failUrl = null;
		
		try
		{
			if(successUrlStr != null)
				successUrl = new URL(successUrlStr);
			if(failUrlStr != null)
				failUrl = new URL(failUrlStr);
		}
		catch(Exception e)
		{
			throw new JsonApiException(ApiErrors.INVALID_URL);
		}
		
		Product prod = ProductManager.getProduct(productId);
		if(prod == null)
		{
			throw new JsonApiException(ApiErrors.NOT_FOUND, "product "+productId+" not found");
		}
		
		Currency currency = CurrencyUtil.getCurrencyForCountry(user.getCountry());
		Integer price = prod.Price.get(currency.getCurrencyCode());
		if(price == null && CurrencyUtil.DefaultCurrency != currency)
		{
			currency = CurrencyUtil.DefaultCurrency;
			price = prod.Price.get(currency.getCurrencyCode());
		}
		
		if(price == null)
			throw new JsonApiException(ApiErrors.NOT_FOUND, "Products didn't have prices in "+CurrencyUtil.DefaultCurrency);
		
		try
		{
			StripeInterface.makePurchase(user, prod, currency, stripeToken);
		}
		catch(Exception e)
		{
			if(failUrl != null)
			{
				String encodedError = null;
				try
				{
					encodedError = URLEncoder.encode(e.getMessage(), "UTF-8");
				}
				catch(Exception e1)
				{
					l.error("Error encoding uri parameter, should not happen?", e1);
					encodedError = "UriEncodingError";
				}
				
				String redirectUrl = failUrlStr;
				if(failUrl.getQuery() != null)
					redirectUrl = failUrlStr + "&error="+encodedError;
				else
					redirectUrl = failUrlStr + "?error="+encodedError;

				return HttpHelpers.sendRedirect(req, redirectUrl);
			}
			throw e;
		}
		
		if(successUrl != null)
			return HttpHelpers.sendRedirect(req, successUrlStr);
		
		return HttpHelpers.sendSuccess(req);
	}
}
