package net.mixednutz.api.feed.client;

import com.andrewfesta.feedme.FeedReader;

import net.mixednutz.api.client.GroupClient;
import net.mixednutz.api.client.MixednutzClient;
import net.mixednutz.api.client.UserClient;

public class FeedAdapter implements MixednutzClient {

	private FeedReader reader;
	private String url;
	private FeedTimelineAdapter timelineAdapter;
	
	public FeedAdapter(FeedReader reader, String url) {
		super();
		this.reader = reader;
		this.url = url;
		initSubApis();
	}

	@Override
	public GroupClient getGroupClient() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FeedTimelineAdapter getTimelineClient() {
		return timelineAdapter;
	}

	@Override
	public UserClient<?> getUserClient() {
		// TODO Auto-generated method stub
		return null;
	}
	
	private void initSubApis() {
		timelineAdapter = new FeedTimelineAdapter(reader, url);
	}

}
