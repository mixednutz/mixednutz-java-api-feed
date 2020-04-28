package net.mixednutz.api.feed.model;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;

import net.mixednutz.api.core.model.TimelineElement;
import net.mixednutz.api.model.ITimelineElement;

@JsonIgnoreProperties(ignoreUnknown=true)
public class FeedEntryElement extends TimelineElement implements ITimelineElement {

	private static final Map<String, TimelineElement.Type> TYPES = new HashMap<>();
	
	private static TimelineElement.Type getType(SyndFeed feed) {
		if (!TYPES.containsKey(feed.getUri())) {
			TimelineElement.Type type = new TimelineElement.Type(){
				FeedElement feedType = new FeedElement(feed);
				@Override
				public String getName() {return "entry";}
				@Override
				public String getNamespace() {return feedType.getHostName();}
				@Override
				public String getId() {return feedType.getId()+"_"+getName();}
				};
			TYPES.put(feed.getUri(), type);
		}
		return TYPES.get(feed.getUri());
	}
	
	String uri;
	Content _description;
	List<Content> contents = new ArrayList<>();
	List<Enclosure> enclosures = new ArrayList<>();
	List<Category> categories = new ArrayList<>();
	
	public FeedEntryElement() {
		super();
	}

	public FeedEntryElement(SyndFeed feed, SyndEntry entry) {
		super();
		this.setType(getType(feed));
		this.setUrl(entry.getLink());
		this.uri = entry.getUri();
		if (uri!=null && uri.startsWith("/")) {
			//fix relative uri
			try {
				this.uri = new URL(new URL(feed.getLink()), this.uri).toExternalForm();
			} catch (MalformedURLException e) {
				// Ignore
			}
		}
		this.setTitle(entry.getTitle());
		if (entry.getDescription()!=null) {
			this._description = new Content(entry.getDescription());
			this.setDescription(this._description.value);
		}
		if (entry.getContents()!=null && !entry.getContents().isEmpty()) {
			for (SyndContent content: entry.getContents()) {
				this.contents.add(new Content(content));
			}
		}
		if (entry.getEnclosures()!=null && entry.getEnclosures().isEmpty()) {
			for (SyndEnclosure enclosure: entry.getEnclosures()) {
				this.enclosures.add(new Enclosure(enclosure));
			}
		}
		if (entry.getCategories()!=null && entry.getCategories().isEmpty()) {
			for (SyndCategory category: entry.getCategories()) {
				this.categories.add(new Category(category));
			}
		}
		if (entry.getAuthor()!=null) {
			this.setPostedByUser(new FeedEntryAuthor(entry.getAuthor()));
		}
		if (entry.getPublishedDate()!=null) {
			this.setPostedOnDate(ZonedDateTime.ofInstant(
					entry.getPublishedDate().toInstant(), ZoneId.systemDefault()));
		}
		if (entry.getUpdatedDate()!=null) {
			this.setUpdatedOnDate(ZonedDateTime.ofInstant(
					entry.getUpdatedDate().toInstant(), ZoneId.systemDefault()));
		}
		if (getPostedOnDate()==null && getUpdatedOnDate()!=null) {
			this.setPostedOnDate(getUpdatedOnDate());
		}
		this.setPaginationId(Long.toString(this.getPostedOnDate().toInstant().toEpochMilli()));
	}
	
	class Content {
		String type;
		String value;
		public Content(SyndContent content) {
			super();
			this.type = content.getType();
			this.value = content.getValue();
		}
		
	}
	
	class Enclosure {
		String type;
		String url;
		public Enclosure(SyndEnclosure enclosure) {
			super();
			this.type = enclosure.getType();
			this.url = enclosure.getUrl();
		}
		
	}
	
}
