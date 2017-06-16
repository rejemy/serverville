package com.dreamwing.serverville.util;

import java.util.Random;

public class RandomUtil
{
	static Random Rand = new Random();
	
	public static int randInt()
	{
		return Rand.nextInt();
	}
	
	public static int randInt(int max)
	{
		return Rand.nextInt(max);
	}
	
	public static int randIntRange(int min, int max)
	{
		return min + Rand.nextInt(max-min);
	}
	
	
}
