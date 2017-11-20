package com.dreamwing.serverville.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

public class CurrencyUtil
{
	public static Currency DefaultCurrency;
	
	public static Currency getCurrencyFromCode(String currencyCode)
	{
		try
		{
			return Currency.getInstance(currencyCode);
		}
		catch(IllegalArgumentException e)
		{
			return null;
		}
	}
	
	public static boolean isValidCurrency(String currencyCode)
	{
		if(currencyCode == null)
			return false;
		
		Currency curr;
		try
		{
			curr = Currency.getInstance(currencyCode);
		}
		catch(IllegalArgumentException e)
		{
			return false;
		}
		
		return curr.getCurrencyCode().equals(currencyCode);
	}
	
	public static Currency getCurrencyForCountry(String countryCode)
	{
		if(countryCode == null)
			return DefaultCurrency;
		
		Currency currency = Currency.getInstance(new Locale("", countryCode));
		if(currency != null)
			return currency;
		
		return DefaultCurrency;
	}
	
	public static BigDecimal getPriceAsDecimal(Currency currency, int price)
	{
		int decimalPoints = currency.getDefaultFractionDigits();
		if(decimalPoints < 0)
		{
			decimalPoints = 0;
		}
		return BigDecimal.valueOf(price, decimalPoints);
	}
	
	public static String getDisplayPrice(Locale loc, Currency currency, int price)
	{
		NumberFormat formatter = NumberFormat.getCurrencyInstance(loc);
		formatter.setCurrency(currency);
		
		return formatter.format(getPriceAsDecimal(currency, price));
	}
	
}
