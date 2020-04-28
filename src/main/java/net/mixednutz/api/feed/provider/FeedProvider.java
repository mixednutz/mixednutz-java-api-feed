package net.mixednutz.api.feed.provider;

import com.andrewfesta.feedme.FeedReader;

import net.mixednutz.api.core.provider.AbstractApiProvider;
import net.mixednutz.api.feed.client.FeedAdapter;
import net.mixednutz.api.model.INetworkInfoSmall;

public class FeedProvider<Credentials> extends AbstractApiProvider<FeedAdapter, Credentials> {

	String url;
	FeedReader reader;
	
	public FeedProvider(FeedReader reader, String url, Class<Credentials> credentialsInterface) {
		super(FeedAdapter.class, credentialsInterface);
		this.url = url;
		this.reader = reader;
	}

	@Override
	public String getProviderId() {
		return url;
	}

	@Override
	public INetworkInfoSmall getNetworkInfo() {
		return getApi(null).getTimelineClient().getFeedElement();
	}

	@Override
	public FeedAdapter getApi(Credentials creds) {
		//TODO one day we have to implement private auth-based feeds
		return new FeedAdapter(reader, url);
	}
	
}
