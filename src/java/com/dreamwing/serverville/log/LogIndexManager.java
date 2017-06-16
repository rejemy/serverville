package com.dreamwing.serverville.log;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;

import com.dreamwing.serverville.net.ApiErrors;
import com.dreamwing.serverville.net.JsonApiException;

public class LogIndexManager
{
	public static class LogSearchHit
	{
		public double timestamp;
		public String log_line;
	}
	
	public static class LogSearchHits
	{
		public List<LogSearchHit> hits;
	}
	
	private static Analyzer TextAnalyzer;
	private static StandardQueryParser Parser = null;
	private static Sort TimestampAscendingSorter;
	private static Sort TimestampDescendingSorter;
	
	private static ConcurrentNavigableMap<String,LuceneIndexWrapper> CurrentIndexes;
	private static boolean HasIndex = false;
	
	public static void init()
	{
		TextAnalyzer = new StandardAnalyzer();
		Parser = new StandardQueryParser(TextAnalyzer);
		
		CurrentIndexes = new ConcurrentSkipListMap<String,LuceneIndexWrapper>();
		
		TimestampAscendingSorter = new Sort(new SortField("timestamp", SortField.Type.LONG));
		TimestampDescendingSorter = new Sort(new SortField("timestamp", SortField.Type.LONG, true));
	}
	
	public static void addIndex(LuceneIndexWrapper index)
	{
		CurrentIndexes.put(index.getFilename(), index);
		HasIndex = true;
	}
	
	public static void removeIndex(LuceneIndexWrapper index)
	{
		CurrentIndexes.remove(index.getFilename());
		HasIndex = !CurrentIndexes.isEmpty();
	}
	
	public static Analyzer getAnalyzer() { return TextAnalyzer; }
	public static Sort getTimestampAscendingSorter() { return TimestampAscendingSorter; }
	public static Sort getTimestampDescendingSorter() { return TimestampDescendingSorter; }
	
	public static LogSearchHits query(String queryStr, long lowerTime, long upperTime, int maxResults, boolean ascending) throws QueryNodeException, IOException, JsonApiException
	{
		Query query = Parser.parse(queryStr, "message");
		
		return query(query, lowerTime, upperTime, maxResults, ascending);
	}
	
	public static LogSearchHits query(Query query, long lowerTime, long upperTime, int maxResults, boolean ascending) throws IOException, JsonApiException
	{
		if(!HasIndex)
			throw new JsonApiException(ApiErrors.NO_INDEXES);
		
		if(lowerTime != Long.MIN_VALUE || upperTime != Long.MAX_VALUE)
		{
			Query rangeQuery = LongPoint.newRangeQuery("timestamp", lowerTime, upperTime);
			BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
			booleanQueryBuilder.add(query, BooleanClause.Occur.MUST);
			booleanQueryBuilder.add(rangeQuery, BooleanClause.Occur.FILTER);
			query = booleanQueryBuilder.build();
		}
		
		NavigableMap<String,LuceneIndexWrapper> indexes = null;
		Sort sorter = null;
		if(ascending)
		{
			indexes = CurrentIndexes;
			sorter = TimestampAscendingSorter;
		}
		else
		{
			indexes = CurrentIndexes.descendingMap();
			sorter = TimestampDescendingSorter;
		}
		
		LogSearchHits hits = new LogSearchHits();
		hits.hits = new LinkedList<LogSearchHit>();
		
		boolean gotHit = false;
		int resultsRemaining = maxResults;
		for(LuceneIndexWrapper index : indexes.values())
		{
			if(!index.overlapsTimePeriod(lowerTime, upperTime))
			{
				if(gotHit) // If we've already found the time window and gone past it, just stop
					break;
				continue;
			}
			
			gotHit = true;
			
			int numHits = index.query(hits.hits, query, resultsRemaining, sorter);
			resultsRemaining -= numHits;
			if(resultsRemaining <= 0)
				break;
		}
		
		return hits;
	}
}
