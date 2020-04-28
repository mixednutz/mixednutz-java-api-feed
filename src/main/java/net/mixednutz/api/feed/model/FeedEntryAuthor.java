package net.mixednutz.api.feed.model;

import com.rometools.rome.feed.synd.SyndPerson;

import net.mixednutz.api.core.model.UserSmall;

public class FeedEntryAuthor extends UserSmall {

	public FeedEntryAuthor() {
		super();
	}
	
	public FeedEntryAuthor(SyndPerson person) {
		super();
//		this.setProviderId(Long.toString(user.getId()));
//		this.setUrl("https://twitter.com/"+user.getScreenName());
//		this.setUri("/users/show.json?screen_name="+user.getScreenName());
//		this.setUsername("@"+user.getScreenName());
//		this.setDisplayName(user.getName());
//		this.setAvatar(new Image(user.getProfileImageURL(), getUsername()+"'s profile image"));
//		this.setPrivate(user.isProtected());
	}
	
	public FeedEntryAuthor(String author) {
		this.setDisplayName(author);
	}

}
