package net.mixednutz.api.feed.model;

import java.net.MalformedURLException;
import java.net.URL;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rometools.rome.feed.synd.SyndFeed;

import net.mixednutz.api.core.model.NetworkInfoSmall;

@JsonIgnoreProperties(ignoreUnknown=true)
public class FeedElement extends NetworkInfoSmall {
	
	private static final String ID = "rss";
	private static final String ICON_NAME = "rss";
	
	public FeedElement(SyndFeed feed) {
		super();
		this.setFontAwesomeIconName(ICON_NAME);
		this.setDisplayName(feed.getTitle());
		this.setId(ID);
		URL url;
		try {
			url = new URL(feed.getLink());
			this.setHostName(url.getHost());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

}
