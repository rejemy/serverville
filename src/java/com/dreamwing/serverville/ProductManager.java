package com.dreamwing.serverville;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.dreamwing.serverville.data.Product;
import com.dreamwing.serverville.util.LocaleUtil;

public class ProductManager
{
	private static ConcurrentMap<String,Product> ProductDb;
	
	public static void init() throws SQLException
	{
		reloadProducts();
	}
	
	public static List<Product> reloadProducts() throws SQLException
	{
		ConcurrentMap<String,Product> productDb = new ConcurrentHashMap<String,Product>();
		
		List<Product> products = Product.loadAll();
		
		for(Product product : products)
		{
			productDb.put(product.ProductId, product);
		}
		
		ProductDb = productDb;
		
		return products;
	}
	
	public static Collection<Product> getProductList()
	{
		return ProductDb.values();
	}
	
	public static Product getProduct(String id)
	{
		return ProductDb.get(id);
	}
	
	public static Product reloadProduct(String id) throws SQLException
	{
		Product product = Product.load(id);
		if(product == null)
			ProductDb.remove(id);
		else
			ProductDb.put(product.ProductId, product);
		
		return product;
	}
	
	// TODO: send reload message to cluster
	public static void addProduct(Product product) throws SQLException
	{
		LocaleUtil.setMapDefaults(product.Text);
		product.save();
		ProductDb.put(product.ProductId, product);
	}
	
	// TODO: send reload message to cluster
	public static void removeProduct(String id) throws SQLException
	{
		ProductDb.remove(id);
		Product.deleteById(id);
	}
}
