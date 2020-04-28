package net.mixednutz.api.feed.model;

import com.rometools.rome.feed.synd.SyndCategory;

public class Category {
	String name;
	String taxonomyUri;
	
	public Category(SyndCategory category) {
		super();
		this.name = category.getName();
		this.taxonomyUri = category.getTaxonomyUri();
	}
	
}
