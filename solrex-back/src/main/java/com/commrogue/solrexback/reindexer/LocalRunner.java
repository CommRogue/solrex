/* (C)Team Eclipse 2024 */
package com.commrogue.solrexback.reindexer;

import com.commrogue.solrexback.common.Collection;
import com.commrogue.solrexback.reindexer.reactive.ReindexJob;
import java.time.LocalDateTime;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("local-runner")
public class LocalRunner implements ApplicationRunner {
    Function<String, CloudSolrClient> cloudSolrClientFactory;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ReindexJob reindexJob = ReindexJob.builder(cloudSolrClientFactory)
                .withTimestampField("bank_date")
                .withStartDate(LocalDateTime.of(2024, 1, 1, 0, 0, 0))
                .withEndDate(LocalDateTime.of(2024, 1, 2, 0, 0, 0))
                .withSrcCollection(new Collection("localhost:2181", "xax"))
                .withDstCollection(new Collection("localhost:2182", "products"))
                .withTimeRangeSplitAmount(5)
                .withSrcDiRequestHandler("diRequestHandler")
                .withIsNatNetworking(true)
                .build();

        Disposable disposable =
                reindexJob.start().subscribeOn(Schedulers.boundedElastic()).subscribe();

        Thread.sleep(5000);

        disposable.dispose();
    }
}
