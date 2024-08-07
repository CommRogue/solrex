package com.commrogue.solrexback.reindexer;

import com.commrogue.solrexback.common.Collection;
import com.commrogue.solrexback.reindexer.reactive.ReindexJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("local-runner")
public class LocalRunner implements ApplicationRunner {
    @Value("${reindexer.dataimport-rh}")
    private String diRequestHandler;

    @Value("${reindexer.networking.nat}")
    private boolean isNatNetworking;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        //        try (CloudLegacySolrClient destinationClient = new CloudLegacySolrClient.Builder(List.of("localhost:2182"), Optional.empty()).build();
//             CloudLegacySolrClient sourceClient = new CloudLegacySolrClient.Builder(List.of("localhost:2181"), Optional.empty()).build()) {
//            var destinationCollection = destinationClient.getClusterState().getCollection("products");
//            var sourceCollection = sourceClient.getClusterState().getCollection("xax");
//            destinationCollection.getActiveSlices().forEach(slice -> {
//                System.out.println(slice.getLeader().getCoreUrl());
//            });
//            System.out.println("ew");
//        }
        ReindexJob reindexJob =
                new ReindexJob("bank_date", LocalDateTime.of(2024, 1, 1, 0, 0, 0), LocalDateTime.of(2024, 1, 2, 0, 0,
                        0),
                        new Collection("localhost:2181", "xax"), new Collection("localhost:2182", "products"), 5,
                        diRequestHandler, isNatNetworking);
        Disposable disposable = reindexJob.run().subscribeOn(Schedulers.boundedElastic()).subscribe();

        // sleep 5 seconds
        Thread.sleep(5000);

        disposable.dispose();
    }
}
