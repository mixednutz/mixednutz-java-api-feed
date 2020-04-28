package net.mixednutz.api.feed.client;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.andrewfesta.feedme.FeedReader;
import com.andrewfesta.feedme.ReadResponse;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;

import net.mixednutz.api.client.TimelineClient;
import net.mixednutz.api.core.model.Page;
import net.mixednutz.api.core.model.PageRequest;
import net.mixednutz.api.feed.model.FeedElement;
import net.mixednutz.api.feed.model.FeedEntryElement;
import net.mixednutz.api.model.IPage;
import net.mixednutz.api.model.IPageRequest;
import net.mixednutz.api.model.IPageRequest.Direction;
import net.mixednutz.api.model.ITimelineElement;

public class FeedTimelineAdapter implements TimelineClient<Long> {

	final FeedReader reader;
	final String url;
	int defaultPageSize = 20;
	int maxPageSize = 200;
	
	public FeedTimelineAdapter(FeedReader reader, String url) {
		super();
		this.reader = reader;
		this.url = url;
	}

	public void setDefaultPageSize(int defaultPageSize) {
		this.defaultPageSize = defaultPageSize;
	}

	public void setMaxPageSize(int maxPageSize) {
		this.maxPageSize = maxPageSize;
	}

	public int getDefaultPageSize() {
		return defaultPageSize;
	}

	public int getMaxPageSize() {
		return maxPageSize;
	}

	protected PageRequest<Long> parseStringPaginationToken(IPageRequest<String> pagination) {
		if (pagination.getStart()!=null) {
			return PageRequest.next(
					Long.valueOf(pagination.getStart()),
					pagination.getPageSize(),
					pagination.getDirection());
		} 
		return PageRequest.first(pagination.getPageSize(), pagination.getDirection(), Long.class);
	}
	
	public FeedElement getFeedElement() {
		ReadResponse<SyndFeed> response = reader.readFeed(url, null);
		if (response.getBody()!=null) {
			return new FeedElement(response.getBody());
		}
		return null;
	}
	
	@Override
	public <T> IPageRequest<T> getTimelinePollRequest(T start) {
		// Get entries from starting time. 
		return PageRequest.next(start, maxPageSize, Direction.GREATER_THAN);
	}

	@Override
	public Page<FeedEntryElement, Long> getTimeline() {
		return getTimeline(null);
	}

	@Override
	public Page<FeedEntryElement, Long> getTimeline(IPageRequest<Long> pagination) {
		final FeedPaging paging = toFeedPaging(pagination);

		paging.pageSize = pagination!=null&&pagination.getPageSize()>0?pagination.getPageSize():defaultPageSize;
		
		ReadResponse<SyndFeed> response = reader.readFeed(url, paging.ifModifiedSince);
		if (response.getBody()!=null) {
			
			List<SyndEntry> results = response.getBody().getEntries()
				.stream()
				.filter((entry)->{
					//Filter for maxDate
					if (paging.maxDate!=null) {
						return entry.getPublishedDate().before(new Date(paging.maxDate));
					}
					return true;
				})
				//TODO filter for hashtag
				.collect(Collectors.toList());
			
			//Trim list to pageSize
			if (!results.isEmpty()) {
				if (results.size()>paging.pageSize) {
					results = results.subList(0, paging.pageSize);
				}
			}
			
			//Wrap in Page
			return toPage(response.getBody(), results, paging, pagination);
		}
		
		return null;
	}

	@Override
	public IPage<? extends ITimelineElement, Long> getTimelineStringToken(IPageRequest<String> pagination) {
		return getTimeline(parseStringPaginationToken(pagination));
	}

	@Override
	public IPage<? extends ITimelineElement, Long> getPublicTimeline() {
		return getPublicTimeline(null);
	}

	@Override
	public IPage<? extends ITimelineElement, Long> getPublicTimeline(IPageRequest<Long> pagination) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPage<? extends ITimelineElement, Long> getPublicTimelineStringToken(IPageRequest<String> pagination) {
		return getPublicTimeline(parseStringPaginationToken(pagination));
	}
	
	FeedPaging toFeedPaging(IPageRequest<Long> pagination) {
		FeedPaging paging = new FeedPaging();
		if (pagination!=null) {
			paging.pageSize = pagination.getPageSize();
			
			if (pagination.getDirection()==Direction.GREATER_THAN) {
				if (pagination.getStart()!=null) {
					paging.ifModifiedSince = pagination.getStart();
				}
			} else {
				if (pagination.getStart()!=null) {
					paging.maxDate = pagination.getStart();
				}
			}
		}
		return paging;
	}
	
	PageRequest<Long> toPageRequest(Integer pageSize, Long maxDate, Long ifModifiedSince) {
		if (maxDate!=null) {
			return PageRequest.next(maxDate, pageSize, Direction.LESS_THAN);
		} 
		if (ifModifiedSince!=null) {
			return PageRequest.next(ifModifiedSince, pageSize, Direction.GREATER_THAN);
		} 
		return null;
	}
	
	PageRequest<Long> toPageRequest(IPageRequest<Long> pageRequest) {
		return PageRequest.next(pageRequest.getStart(), pageRequest.getPageSize(), 
				pageRequest.getDirection());
	}
	
	Page<FeedEntryElement, Long> toPage(SyndFeed feed, List<SyndEntry> items, 
			FeedPaging prevPage, IPageRequest<Long> pageRequest) {
		LinkedList<FeedEntryElement> newItemList = new LinkedList<>();
		for (SyndEntry entry: items) {
			newItemList.add(new FeedEntryElement(feed, entry));
		}
		
		Page<FeedEntryElement, Long> newPage = new Page<>();
		newPage.setItems(newItemList);
		if (pageRequest!=null) {
			newPage.setPageRequest(toPageRequest(pageRequest));
		}
		if (!items.isEmpty()) {
			int pageSize =prevPage!=null&&prevPage.pageSize!=null?prevPage.pageSize:items.size();
			Long last = Long.valueOf(newItemList.getLast().getPaginationId());
			Long first = Long.valueOf(newItemList.getFirst().getPaginationId());
			/*
			 * If pageRequest is null, the request got ALL items.  There is no NEXT Page.
			 */
			if ((pageRequest!=null && Direction.GREATER_THAN.equals(pageRequest.getDirection())) ||
					(prevPage.maxDate==null && prevPage.ifModifiedSince!=null)) {
				newPage.setNextPage(toPageRequest(pageSize, null, first));
				newPage.setReversePage(toPageRequest(pageSize, last, null));
			} else {
				newPage.setNextPage(toPageRequest(pageSize, last, null));
				newPage.setReversePage(toPageRequest(pageSize, null, first));
			} 
			newPage.setHasNext(true);
			newPage.setHasReverse(true);
		}
		return newPage;
	}
	
	private class FeedPaging {
		Integer pageSize;
		Long maxDate;
		Long ifModifiedSince;
	}

}
