package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;

public class CrawlInternal extends RecursiveAction {
    private final Clock clock;
    private final Duration timeout;
    private final Instant deadlinee;
    private final int popularWordCount;
    private final PageParserFactory parserFactory;
    private final int maxDepth;
    private final List<Pattern> ignoredUrls;
    private final Set<String> visitedUrls;
    private final String url;
    private final Map<String, Integer> counts;

    public CrawlInternal(Clock clock, Duration timeout, int popularWordCount,PageParserFactory parserFactory, int maxDepth, List<Pattern> ignoredUrls, Set<String> visitedUrls, String url, Map<String, Integer> counts) {
        this.clock = clock;
        this.timeout = timeout;
        this.parserFactory = parserFactory;
        this.popularWordCount = popularWordCount;
        this.maxDepth = maxDepth;
        this.ignoredUrls = ignoredUrls;
        this.visitedUrls = visitedUrls;
        this.url = url;
        this.counts = counts;
        deadlinee = null;
    }

    public CrawlInternal(Clock clock, Instant deadlinee, int popularWordCount,PageParserFactory parserFactory, int maxDepth, List<Pattern> ignoredUrls, Set<String> visitedUrls, String url, Map<String, Integer> counts) {
        this.clock = clock;
        this.deadlinee = deadlinee;
        this.parserFactory = parserFactory;
        this.popularWordCount = popularWordCount;
        this.maxDepth = maxDepth;
        this.ignoredUrls = ignoredUrls;
        this.visitedUrls = visitedUrls;
        this.url = url;
        this.counts = counts;
        timeout = null;
    }

    /**
     * The main computation performed by this task.
     */
    @Override
    protected void compute() {
        Instant deadline;
        if(timeout != null) {
            deadline = clock.instant().plus(timeout);
        }
        else {
            deadline = deadlinee;
        }
        if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
            return;
        }
        for (Pattern pattern : ignoredUrls) {
            if (pattern.matcher(url).matches()) {
                return;
            }
        }
        if (!visitedUrls.add(url)) {
            return;
        }
       // visitedUrls.add(url);
        PageParser.Result result = parserFactory.get(url).parse();
        for (ConcurrentMap.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
           /* if (counts.containsKey(e.getKey())) {
                counts.put(e.getKey(), e.getValue() + counts.get(e.getKey()));
            } else {
                counts.put(e.getKey(), e.getValue());
            }*/

            counts.compute(e.getKey(), (keyId, wordCount) -> {
                if (Objects.nonNull(wordCount)) {
                    return wordCount + e.getValue();
                } return e.getValue();
            });
        }
        List<CrawlInternal> recursiveTasks = new ArrayList<>();
        for (String link : result.getLinks()) {
            recursiveTasks.add(
            new CrawlInternal(clock, deadline, popularWordCount, parserFactory, maxDepth-1, ignoredUrls,
                    visitedUrls, link, counts));
        }
        invokeAll(recursiveTasks);
    }
}


