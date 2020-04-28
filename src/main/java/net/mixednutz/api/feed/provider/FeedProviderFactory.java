package net.mixednutz.api.feed.provider;

import com.andrewfesta.feedme.FeedReader;

public class FeedProviderFactory {
	
	private FeedReader feedReader;

	public FeedProviderFactory(FeedReader feedReader) {
		super();
		this.feedReader = feedReader;
	}

	public FeedProvider<?> connect(String url) {
		return new FeedProvider<Void>(feedReader, url, Void.class);
	}
	
}
