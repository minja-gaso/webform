package org.sw.marketing.servlet;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class DemoClass
{
	public static void main(String[] args)
	{
		ListMultimap<Long, String> multimap = ArrayListMultimap.create();
		multimap.put((long) 1, "One");
		multimap.put((long) 1, "Two");
		
		System.out.println(multimap.get((long) 1));
	}
}
