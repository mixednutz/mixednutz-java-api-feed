package com.andrewfesta.feedme;

import com.rometools.rome.feed.synd.SyndFeed;

public interface FeedReader {

	ReadResponse<SyndFeed> readFeed(String url, Long ifModifiedSince);
	
}
