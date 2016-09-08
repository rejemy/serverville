package com.dreamwing.serverville.util;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CurrencyUtil
{
	private static Map<String,String> CountryToCurrency;
	private static Set<String> Currencies;
	
	public static String DefaultCurrency;
	
	public static boolean isValidCountry(String countryCode)
	{
		if(countryCode == null)
			return false;
		
		return CountryToCurrency.containsKey(countryCode);
	}
	
	public static boolean isValidCurrency(String currencyCode)
	{
		if(currencyCode == null)
			return false;
		
		return Currencies.contains(currencyCode);
	}
	
	public static String getCurrency(String countryCode)
	{
		if(countryCode == null)
			return DefaultCurrency;
		
		String currency = CountryToCurrency.get(countryCode.toUpperCase());
		if(currency != null)
			return currency;
		
		return DefaultCurrency;
	}
	
	public static String getDisplayPrice(Locale loc, String currency, int price)
	{
		currency = currency.toUpperCase();
		NumberFormat formatter = NumberFormat.getNumberInstance(loc);
		
		switch(currency)
		{
			case "USD":
			case "CAD":
			case "AUD":
			case "NZD":
			case "MXN":
			case "HKD":
			case "SGD":
				return "$"+formatCents(formatter, price);
			case "EUR":
				return "€"+formatCents(formatter, price);
			case "GBP":
			case "FKP":
			case "GIP":
			case "GGP":
			case "IMP":
			case "JEP":
			case "SHP":
				return "£"+formatCents(formatter, price);
			case "JPY":
				return "¥"+format(formatter, price);
			case "CNY":
				return "¥"+formatCents(formatter, price);
			case "NOK":
			case "SEK":
				return formatCents(formatter, price)+ " kr";
			case "CHF":
				return formatCents(formatter, price)+ " fr";
			case "RUB":
				return "₽"+formatCents(formatter, price);
			case "TRY":
				return "₺"+formatCents(formatter, price);
			case "KRW":
			case "KPW":
				return "₩"+format(formatter, price);
			case "ZAR":
				return formatCents(formatter, price)+ " R";
			case "BRL":
				return "R$"+formatCents(formatter, price);
			case "INR":
				return "₹"+formatCents(formatter, price);
		}
		
		Currency curr = Currency.getInstance(currency.toUpperCase());
		NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(loc);
		currencyFormatter.setCurrency(curr);
		double dollarPrice = price / 100.0;
		
		return currencyFormatter.format(dollarPrice);
	}
	
	
	private static String formatCents(NumberFormat formatter, int price)
	{
		int dollars = Math.floorDiv(price, 100);
		int cents = Math.floorMod(price, 100);
		
		if(cents == 0)
		{
			return formatter.format(dollars);
		}
		else
		{
			return formatter.format(price / 100.0);
		}
		
		
	}
	
	private static String format(NumberFormat formatter, int price)
	{
		return formatter.format(price);
	}
	
	private static void buildCurrenciesList()
	{
		Currencies = new HashSet<String>();
		for(String currency : CountryToCurrency.values())
		{
			Currencies.add(currency);
		}
	}
	
	static
	{
		CountryToCurrency = new HashMap<String,String>();
		CountryToCurrency.put("NZ","NZD");
		CountryToCurrency.put("CK","NZD");
		CountryToCurrency.put("NU","NZD");
		CountryToCurrency.put("PN","NZD");
		CountryToCurrency.put("TK","NZD");
		CountryToCurrency.put("AU","AUD");
		CountryToCurrency.put("CX","AUD");
		CountryToCurrency.put("CC","AUD");
		CountryToCurrency.put("HM","AUD");
		CountryToCurrency.put("KI","AUD");
		CountryToCurrency.put("NR","AUD");
		CountryToCurrency.put("NF","AUD");
		CountryToCurrency.put("TV","AUD");
		CountryToCurrency.put("AS","EUR");
		CountryToCurrency.put("AD","EUR");
		CountryToCurrency.put("AT","EUR");
		CountryToCurrency.put("BE","EUR");
		CountryToCurrency.put("FI","EUR");
		CountryToCurrency.put("FR","EUR");
		CountryToCurrency.put("GF","EUR");
		CountryToCurrency.put("TF","EUR");
		CountryToCurrency.put("DE","EUR");
		CountryToCurrency.put("GR","EUR");
		CountryToCurrency.put("GP","EUR");
		CountryToCurrency.put("IE","EUR");
		CountryToCurrency.put("IT","EUR");
		CountryToCurrency.put("LU","EUR");
		CountryToCurrency.put("MQ","EUR");
		CountryToCurrency.put("YT","EUR");
		CountryToCurrency.put("MC","EUR");
		CountryToCurrency.put("NL","EUR");
		CountryToCurrency.put("PT","EUR");
		CountryToCurrency.put("RE","EUR");
		CountryToCurrency.put("WS","EUR");
		CountryToCurrency.put("SM","EUR");
		CountryToCurrency.put("SI","EUR");
		CountryToCurrency.put("ES","EUR");
		CountryToCurrency.put("VA","EUR");
		CountryToCurrency.put("GS","GBP");
		CountryToCurrency.put("GB","GBP");
		CountryToCurrency.put("JE","GBP");
		CountryToCurrency.put("IO","USD");
		CountryToCurrency.put("GU","USD");
		CountryToCurrency.put("MH","USD");
		CountryToCurrency.put("FM","USD");
		CountryToCurrency.put("MP","USD");
		CountryToCurrency.put("PW","USD");
		CountryToCurrency.put("PR","USD");
		CountryToCurrency.put("TC","USD");
		CountryToCurrency.put("US","USD");
		CountryToCurrency.put("UM","USD");
		CountryToCurrency.put("VG","USD");
		CountryToCurrency.put("VI","USD");
		CountryToCurrency.put("HK","HKD");
		CountryToCurrency.put("CA","CAD");
		CountryToCurrency.put("JP","JPY");
		CountryToCurrency.put("AF","AFN");
		CountryToCurrency.put("AL","ALL");
		CountryToCurrency.put("DZ","DZD");
		CountryToCurrency.put("AI","XCD");
		CountryToCurrency.put("AG","XCD");
		CountryToCurrency.put("DM","XCD");
		CountryToCurrency.put("GD","XCD");
		CountryToCurrency.put("MS","XCD");
		CountryToCurrency.put("KN","XCD");
		CountryToCurrency.put("LC","XCD");
		CountryToCurrency.put("VC","XCD");
		CountryToCurrency.put("AR","ARS");
		CountryToCurrency.put("AM","AMD");
		CountryToCurrency.put("AW","ANG");
		CountryToCurrency.put("AN","ANG");
		CountryToCurrency.put("AZ","AZN");
		CountryToCurrency.put("BS","BSD");
		CountryToCurrency.put("BH","BHD");
		CountryToCurrency.put("BD","BDT");
		CountryToCurrency.put("BB","BBD");
		CountryToCurrency.put("BY","BYR");
		CountryToCurrency.put("BZ","BZD");
		CountryToCurrency.put("BJ","XOF");
		CountryToCurrency.put("BF","XOF");
		CountryToCurrency.put("GW","XOF");
		CountryToCurrency.put("CI","XOF");
		CountryToCurrency.put("ML","XOF");
		CountryToCurrency.put("NE","XOF");
		CountryToCurrency.put("SN","XOF");
		CountryToCurrency.put("TG","XOF");
		CountryToCurrency.put("BM","BMD");
		CountryToCurrency.put("BT","INR");
		CountryToCurrency.put("IN","INR");
		CountryToCurrency.put("BO","BOB");
		CountryToCurrency.put("BW","BWP");
		CountryToCurrency.put("BV","NOK");
		CountryToCurrency.put("NO","NOK");
		CountryToCurrency.put("SJ","NOK");
		CountryToCurrency.put("BR","BRL");
		CountryToCurrency.put("BN","BND");
		CountryToCurrency.put("BG","BGN");
		CountryToCurrency.put("BI","BIF");
		CountryToCurrency.put("KH","KHR");
		CountryToCurrency.put("CM","XAF");
		CountryToCurrency.put("CF","XAF");
		CountryToCurrency.put("TD","XAF");
		CountryToCurrency.put("CG","XAF");
		CountryToCurrency.put("GQ","XAF");
		CountryToCurrency.put("GA","XAF");
		CountryToCurrency.put("CV","CVE");
		CountryToCurrency.put("KY","KYD");
		CountryToCurrency.put("CL","CLP");
		CountryToCurrency.put("CN","CNY");
		CountryToCurrency.put("CO","COP");
		CountryToCurrency.put("KM","KMF");
		CountryToCurrency.put("CD","CDF");
		CountryToCurrency.put("CR","CRC");
		CountryToCurrency.put("HR","HRK");
		CountryToCurrency.put("CU","CUP");
		CountryToCurrency.put("CY","CYP");
		CountryToCurrency.put("CZ","CZK");
		CountryToCurrency.put("DK","DKK");
		CountryToCurrency.put("FO","DKK");
		CountryToCurrency.put("GL","DKK");
		CountryToCurrency.put("DJ","DJF");
		CountryToCurrency.put("DO","DOP");
		CountryToCurrency.put("TP","IDR");
		CountryToCurrency.put("ID","IDR");
		CountryToCurrency.put("EC","ECS");
		CountryToCurrency.put("EG","EGP");
		CountryToCurrency.put("SV","SVC");
		CountryToCurrency.put("ER","ETB");
		CountryToCurrency.put("ET","ETB");
		CountryToCurrency.put("EE","EEK");
		CountryToCurrency.put("FK","FKP");
		CountryToCurrency.put("FJ","FJD");
		CountryToCurrency.put("PF","XPF");
		CountryToCurrency.put("NC","XPF");
		CountryToCurrency.put("WF","XPF");
		CountryToCurrency.put("GM","GMD");
		CountryToCurrency.put("GE","GEL");
		CountryToCurrency.put("GI","GIP");
		CountryToCurrency.put("GT","GTQ");
		CountryToCurrency.put("GN","GNF");
		CountryToCurrency.put("GY","GYD");
		CountryToCurrency.put("HT","HTG");
		CountryToCurrency.put("HN","HNL");
		CountryToCurrency.put("HU","HUF");
		CountryToCurrency.put("IS","ISK");
		CountryToCurrency.put("IR","IRR");
		CountryToCurrency.put("IQ","IQD");
		CountryToCurrency.put("IL","ILS");
		CountryToCurrency.put("JM","JMD");
		CountryToCurrency.put("JO","JOD");
		CountryToCurrency.put("KZ","KZT");
		CountryToCurrency.put("KE","KES");
		CountryToCurrency.put("KP","KPW");
		CountryToCurrency.put("KR","KRW");
		CountryToCurrency.put("KW","KWD");
		CountryToCurrency.put("KG","KGS");
		CountryToCurrency.put("LA","LAK");
		CountryToCurrency.put("LV","LVL");
		CountryToCurrency.put("LB","LBP");
		CountryToCurrency.put("LS","LSL");
		CountryToCurrency.put("LR","LRD");
		CountryToCurrency.put("LY","LYD");
		CountryToCurrency.put("LI","CHF");
		CountryToCurrency.put("CH","CHF");
		CountryToCurrency.put("LT","LTL");
		CountryToCurrency.put("MO","MOP");
		CountryToCurrency.put("MK","MKD");
		CountryToCurrency.put("MG","MGA");
		CountryToCurrency.put("MW","MWK");
		CountryToCurrency.put("MY","MYR");
		CountryToCurrency.put("MV","MVR");
		CountryToCurrency.put("MT","MTL");
		CountryToCurrency.put("MR","MRO");
		CountryToCurrency.put("MU","MUR");
		CountryToCurrency.put("MX","MXN");
		CountryToCurrency.put("MD","MDL");
		CountryToCurrency.put("MN","MNT");
		CountryToCurrency.put("MA","MAD");
		CountryToCurrency.put("EH","MAD");
		CountryToCurrency.put("MZ","MZN");
		CountryToCurrency.put("MM","MMK");
		CountryToCurrency.put("NA","NAD");
		CountryToCurrency.put("NP","NPR");
		CountryToCurrency.put("NI","NIO");
		CountryToCurrency.put("NG","NGN");
		CountryToCurrency.put("OM","OMR");
		CountryToCurrency.put("PK","PKR");
		CountryToCurrency.put("PA","PAB");
		CountryToCurrency.put("PG","PGK");
		CountryToCurrency.put("PY","PYG");
		CountryToCurrency.put("PE","PEN");
		CountryToCurrency.put("PH","PHP");
		CountryToCurrency.put("PL","PLN");
		CountryToCurrency.put("QA","QAR");
		CountryToCurrency.put("RO","RON");
		CountryToCurrency.put("RU","RUB");
		CountryToCurrency.put("RW","RWF");
		CountryToCurrency.put("ST","STD");
		CountryToCurrency.put("SA","SAR");
		CountryToCurrency.put("SC","SCR");
		CountryToCurrency.put("SL","SLL");
		CountryToCurrency.put("SG","SGD");
		CountryToCurrency.put("SK","SKK");
		CountryToCurrency.put("SB","SBD");
		CountryToCurrency.put("SO","SOS");
		CountryToCurrency.put("ZA","ZAR");
		CountryToCurrency.put("LK","LKR");
		CountryToCurrency.put("SD","SDG");
		CountryToCurrency.put("SR","SRD");
		CountryToCurrency.put("SZ","SZL");
		CountryToCurrency.put("SE","SEK");
		CountryToCurrency.put("SY","SYP");
		CountryToCurrency.put("TW","TWD");
		CountryToCurrency.put("TJ","TJS");
		CountryToCurrency.put("TZ","TZS");
		CountryToCurrency.put("TH","THB");
		CountryToCurrency.put("TO","TOP");
		CountryToCurrency.put("TT","TTD");
		CountryToCurrency.put("TN","TND");
		CountryToCurrency.put("TR","TRY");
		CountryToCurrency.put("TM","TMT");
		CountryToCurrency.put("UG","UGX");
		CountryToCurrency.put("UA","UAH");
		CountryToCurrency.put("AE","AED");
		CountryToCurrency.put("UY","UYU");
		CountryToCurrency.put("UZ","UZS");
		CountryToCurrency.put("VU","VUV");
		CountryToCurrency.put("VE","VEF");
		CountryToCurrency.put("VN","VND");
		CountryToCurrency.put("YE","YER");
		CountryToCurrency.put("ZM","ZMK");
		CountryToCurrency.put("ZW","ZWD");
		CountryToCurrency.put("AX","EUR");
		CountryToCurrency.put("AO","AOA");
		CountryToCurrency.put("AQ","AQD");
		CountryToCurrency.put("BA","BAM");
		CountryToCurrency.put("GH","GHS");
		CountryToCurrency.put("GG","GGP");
		CountryToCurrency.put("IM","GBP");
		CountryToCurrency.put("ME","EUR");
		CountryToCurrency.put("PS","JOD");
		CountryToCurrency.put("BL","EUR");
		CountryToCurrency.put("SH","GBP");
		CountryToCurrency.put("MF","ANG");
		CountryToCurrency.put("PM","EUR");
		CountryToCurrency.put("RS","RSD");
		
		buildCurrenciesList();
	}
}
