package com.andrewfesta.feedme.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.xml.sax.InputSource;

import com.andrewfesta.feedme.FeedReader;
import com.andrewfesta.feedme.ReadResponse;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;

public class SimpleFeedReader implements FeedReader {
	
	private static final Logger LOG = LoggerFactory.getLogger(SimpleFeedReader.class);

	private static final MediaType[] VALID_MEDIA_TYPES = new MediaType[]{
			MediaType.parseMediaType("application/rss+xml"),
			MediaType.APPLICATION_ATOM_XML, 
			MediaType.TEXT_XML, MediaType.APPLICATION_XML
			};
	
	private static final List<MediaType> ACCEPT_MEDIA_TYPES = Arrays.asList(new MediaType[]{
			MediaType.parseMediaType("application/rss+xml"),
			MediaType.parseMediaType("application/rdf_xml;q=0.8"), 
			MediaType.parseMediaType("application/atom+xml;q=0.6"), 
			MediaType.parseMediaType("application/xml;q=0.4"),
			MediaType.parseMediaType("text/xml;q=0.4") 
			});
	
	private static final String CRAWLER_USER_AGENT = "Feedmebot/1.0";

	@Override
	public ReadResponse<SyndFeed> readFeed(String url, Long ifModifiedSince) {
		CrawlerContext crawlerContext = new CrawlerContext(url, ifModifiedSince);
		crawlerContext.read();
		return crawlerContext.crawlerResponse;
	}
	
	private class CrawlerContext {
		private RestTemplate rest;
		private String requestedUrl;
		private String currentUrl;
		private ResponseEntity<?> responseEntity;
		private ReadResponse<SyndFeed> crawlerResponse;
		private Long ifModifiedSince;
		
		CrawlerContext(String url, Long ifModifiedSince) {
			super();
			this.requestedUrl = url;
			this.rest = new RestTemplate();
			this.ifModifiedSince = ifModifiedSince;
		}
		
		public void read() {
			if (ifModifiedSince==null) {
				LOG.info("Crawling: {}", requestedUrl);
			} else {
				LOG.info("Crawling: {}; lastModified: {}({})", 
						requestedUrl, ifModifiedSince, Instant.ofEpochMilli(ifModifiedSince));
			}
			
			crawlerResponse = new ReadResponse<>();
			crawlerResponse.setCrawlerName("");
			try {
				if (isValidFeed()) {
					if (!isModified()) {
						LOG.info("Feed {} reports no changes since {}({})", 
								currentUrl);
						handleHttpNotModified();
					} else {
						readFeed(currentUrl, crawlerResponse);
						if (crawlerResponse.getBody()!=null && crawlerResponse.getBody().getEntries()!=null) {
							LOG.info("Feed {} returned {} entries", 
									currentUrl, crawlerResponse.getBody().getEntries().size());
						} else {
							LOG.info("Feed {} was unable to be read: {}", currentUrl);
						}
					}
				} else {
					LOG.info("Bad XML: {}", currentUrl);
				}
			} catch (Exception e) {
				LOG.error("Uncaught Exception", e);
				handleException(e);
			}
		}
		
		private HttpHeaders doRequestForHeaders(URI uri, HttpMethod method) {
			HttpHeaders headers = standardHeaders();
			headers.setAccept(ACCEPT_MEDIA_TYPES);
			HttpEntity<?> requestEntity = new HttpEntity<String>(headers);
			if (HttpMethod.HEAD.equals(method)) {
				LOG.debug("Execute: {} {}", HttpMethod.HEAD, uri);
				responseEntity = rest.exchange(uri, HttpMethod.HEAD, requestEntity, String.class);
				return responseEntity.getHeaders();
			}
			headers.add("X-Description","I'm resending this as a GET because your HEAD response isnt acknowleging my ACCEPT header");
			LOG.debug("Execute: {} {}", method, uri);
			responseEntity = rest.exchange(uri, method, requestEntity, String.class);
			LOG.debug("Body:\n{}", requestEntity.getBody());
			return responseEntity.getHeaders();
		}
		
		private HttpHeaders feedHead() {
			return feedHead(HttpMethod.HEAD);
		}
		
		private HttpHeaders feedHead(HttpMethod method) {
			URI uri;
			URL url;
			try {
				uri = new URI(requestedUrl);
				url = uri.toURL();
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
			currentUrl = uri.toString();
			crawlerResponse.setUrl(url);
			return doRequestForHeaders(uri, method);
		}
		
		private boolean isModified() {
			return (responseEntity==null || !HttpStatus.NOT_MODIFIED.equals(responseEntity.getStatusCode()));
		}
		
		private boolean isValidFeed() {
			HttpHeaders headHeaders = null;
			try {
				headHeaders = feedHead();
			} catch (HttpStatusCodeException e) {
				LOG.debug("Unable to run HEAD on "+ currentUrl, e);
				if (!HttpStatus.TOO_MANY_REQUESTS.equals(e.getStatusCode())) {
					//Maybe this service isnt configured to use head. try again using get
					try {
						headHeaders = feedHead(HttpMethod.GET);
						LOG.info("Unable to run HEAD on {} but we were able to run get on it instead", currentUrl);
					} catch (HttpStatusCodeException e2) {
						LOG.debug("Unable to run GET on {} {}\n{}", currentUrl, e2.getStatusCode(), e2.getResponseBodyAsString());
						LOG.info("Unable to run HEAD on "+ currentUrl, e);
						handleHttpStatusCodeException(e);
						return false;
					} catch (Exception e2) {
						LOG.debug("Unable to run GET on "+ currentUrl, e2);
						LOG.info("Unable to run head on ", currentUrl, e);
						handleHttpStatusCodeException(e);
						return false;
					}
				} else {
					//if tooManyRequests we're just going to give up
					handleHttpStatusCodeException(e);
					return false;
				}
			} catch (Exception e) {
				LOG.info("Unable to run HEAD on " + currentUrl, e);
				handleException(e);
				return false;
			}
			if (isModified()) {
				MediaType contentType = headHeaders.getContentType();
				LOG.debug("{} contentType:{}", currentUrl, contentType);
				boolean valid = isCompatibleWith(contentType, VALID_MEDIA_TYPES);
				if (!valid) {
					LOG.info("{} [{}] is not an XML content type. Seeing if GET fixes it", currentUrl, contentType);
					try {
						headHeaders = feedHead(HttpMethod.GET);
						contentType = headHeaders.getContentType();
					} catch (Exception e) {
						//Don't care.  Swallow exception
						LOG.info("Unable to run GET on "+currentUrl, e);
					}
					valid = isCompatibleWith(contentType, VALID_MEDIA_TYPES);
					if (!valid) {
						LOG.info("{} [{}] is not an XML content type.", currentUrl, contentType);
						return valid;
					}
				}
			}
			return true;
		}
		
		private HttpHeaders standardHeaders() {
			HttpHeaders headers = new HttpHeaders();
			headers.add("User-Agent", CRAWLER_USER_AGENT);
			if (ifModifiedSince!=null) {
				headers.setIfModifiedSince(ifModifiedSince);
				LOG.debug("headers: {}",headers);
			}
			return headers;
		}
		
		private boolean isCompatibleWith(MediaType contentType, MediaType[] validMediaTypes) {
			for (MediaType validMediaType: validMediaTypes) {
				if (contentType.isCompatibleWith(validMediaType)) {
					return true;
				}
			}
			return false;
		}
		
		private void readFeed(String urlString, final ReadResponse<SyndFeed> crawlerResponse) {
			final URL url;
			final URI uri;
			try {
				uri = new URI(urlString);
				url = uri.toURL();
			} catch (Exception e) {
				LOG.error("BAD URL", e);
				return;
			} 
			crawlerResponse.setUrl(url);
			Instant startTime = Instant.now();
			
			HttpHeaders headers = standardHeaders();
			headers.setAccept(ACCEPT_MEDIA_TYPES);
			HttpEntity<?> requestEntity = new HttpEntity<String>(headers);
			final ResponseEntity<String> responseEntity = rest.exchange(uri, HttpMethod.GET, requestEntity, String.class);
			this.responseEntity = responseEntity;
			
			//TODO Abstract all of this into its own AbstractHttpMessageConverter
			ByteArrayInputStream bais = new ByteArrayInputStream(responseEntity.getBody().getBytes());
			Function<InputStream, SyndFeed> callback = new Function<InputStream, SyndFeed>() {
				@Override
				public SyndFeed apply(InputStream t) {
					crawlerResponse.setStatusCode(responseEntity.getStatusCodeValue());
					crawlerResponse.setStatusMessage(responseEntity.getStatusCode().getReasonPhrase());
					crawlerResponse.setLastModified(responseEntity.getHeaders().getLastModified());
					crawlerResponse.setCrawledDateTime(ZonedDateTime.now());
					SyndFeedInput in = new SyndFeedInput();
					InputSource is = new InputSource(t);
					try {
						return in.build(is);
					} catch (FeedException e) {
						LOG.debug("Unable to parse feed "+url, e);
						LOG.warn("Unable to parse feed {} because {}", uri, e.getMessage());
						crawlerResponse.setStatusMessage(e.getMessage());
						crawlerResponse.setStatusCode(HttpStatus.NOT_ACCEPTABLE.value());
						return null;
					} catch (Exception e) {
						LOG.error("Unexpected error with feed "+url, e);
						crawlerResponse.setStatusMessage(e.getMessage());
						crawlerResponse.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
						return null;
					}
				}
				
			};

			crawlerResponse.setBody(callback.apply(bais));
			if (crawlerResponse.getBody()!=null) {
				LOG.trace("Read {}: {}", url, crawlerResponse.getBody().getTitle());
			}
			Instant stopTime = Instant.now();
			crawlerResponse.setLatency(Duration.between(startTime, stopTime).toMillis());
		}
		
		private void handleHttpStatusCodeException(HttpStatusCodeException e) {
			crawlerResponse.setStatusCode(e.getStatusCode().value());
			crawlerResponse.setStatusMessage(e.getStatusText());
			crawlerResponse.setCrawledDateTime(ZonedDateTime.now());
		}
		
		private void handleException(Exception e) {
			crawlerResponse.setStatusCode(0);
			crawlerResponse.setStatusMessage(e.getMessage());
			crawlerResponse.setCrawledDateTime(ZonedDateTime.now());
		}
		
		private void handleHttpNotModified() {
			crawlerResponse.setStatusCode(HttpStatus.NOT_MODIFIED.value());
			crawlerResponse.setStatusMessage(HttpStatus.NOT_MODIFIED.getReasonPhrase());
			crawlerResponse.setCrawledDateTime(ZonedDateTime.now());
		}
	}
	
}
