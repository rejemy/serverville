package com.dreamwing.serverville.log;

import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;

public class LogIndexManager
{
	public static class LogSearchHit
	{
		public double timestamp;
		public String message;
		public int file_pos;
	}
	
	public static class LogSearchHits
	{
		public List<LogSearchHit> hits;
	}
	
	private static Analyzer TextAnalyzer;
	private static QueryParser Parser = null;
	private static Sort TimestampSorter;
	
	public static void init()
	{
		TextAnalyzer = new StandardAnalyzer();
		Parser = new QueryParser("all", TextAnalyzer);
		TimestampSorter = new Sort(new SortField("timestamp", SortField.Type.LONG));
	}
	
	public static Analyzer getAnalyzer() { return TextAnalyzer; }
	public static Sort getTimestampSorter() { return TimestampSorter; }
	
	public static LogSearchHits query(String queryStr, long lowerTime, long upperTime) throws ParseException
	{
		Query query = Parser.parse(queryStr);
		
		return query(query, lowerTime, upperTime);
	}
	
	public static LogSearchHits query(Query query, long lowerTime, long upperTime)
	{
		LogSearchHits hits = new LogSearchHits();
		hits.hits = new LinkedList<LogSearchHit>();
		
		return hits;
	}
}
