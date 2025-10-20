package com.jeet.db;

public class Enums {

	public enum SortOrder {
		
		  ASC("asc"),
		  DESC("desc");

		  public String sorting;

		  SortOrder (String order) {
		  		sorting = order;
		  	}

		  	public String get() {
			  return sorting;
			}
		  	
	}
	
}
