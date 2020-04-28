package net.mixednutz.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.andrewfesta.feedme.FeedReader;

import net.mixednutz.api.feed.provider.FeedProviderFactory;

@Configuration
@Profile("rss")
public class FeedsService {
	
	@Bean
	public FeedProviderFactory feedProviderFactory(FeedReader reader) {
		return new FeedProviderFactory(reader);
	}
	
}
