package com.andrewfesta.feedme;

import java.net.URL;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ReadResponse<T> {
	
	private URL url;
	private int statusCode;
	private String statusMessage;
	private long latency;
	private ZonedDateTime crawledDateTime;
	private long lastModified;
	private String crawlerName;
	private T body;
	private String redirectLocation;
	
	public URL getUrl() {
		return url;
	}
	public void setUrl(URL url) {
		this.url = url;
	}
	public int getStatusCode() {
		return statusCode;
	}
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
	public String getStatusMessage() {
		return statusMessage;
	}
	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
	}
	public long getLatency() {
		return latency;
	}
	public void setLatency(long latency) {
		this.latency = latency;
	}
	public ZonedDateTime getCrawledDateTime() {
		return crawledDateTime;
	}
	public void setCrawledDateTime(ZonedDateTime crawledDateTime) {
		this.crawledDateTime = crawledDateTime;
	}
	public long getLastModified() {
		return lastModified;
	}
	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}
	public String getCrawlerName() {
		return crawlerName;
	}
	public void setCrawlerName(String crawlerName) {
		this.crawlerName = crawlerName;
	}
	@JsonIgnore
	public T getBody() {
		return body;
	}
	public void setBody(T body) {
		this.body = body;
	}
	public String getRedirectLocation() {
		return redirectLocation;
	}
	public void setRedirectLocation(String redirectLocation) {
		this.redirectLocation = redirectLocation;
	}
		
}
