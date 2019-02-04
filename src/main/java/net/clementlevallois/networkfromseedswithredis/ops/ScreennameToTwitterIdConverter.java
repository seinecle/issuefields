/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.networkfromseedswithredis.ops;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.clementlevallois.datamining.graph.GraphOperations;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

/**
 *
 * @author LEVALLOIS
 */
public class ScreennameToTwitterIdConverter {

    TransportClient client;
    ObjectMapper mapper;
    String[] screenNames;

    public ScreennameToTwitterIdConverter(String[] screenNames, TransportClient client, ObjectMapper mapper) {
        this.screenNames = screenNames;
        this.client = client;
        this.mapper = mapper;
    }

    public Map<String, String> searchScreenNameReturnsUserId(String[] screenNames) throws IOException {

        Map<String,String> id2ScreenNames= new HashMap();

        int i = 0;
        for (String screenName : screenNames) {

            System.out.println("searching " + screenName);
            screenName = screenName.replace("@", "");
            SearchResponse response = client.prepareSearch("twitter")
                    .setTypes("user")
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .setQuery(QueryBuilders.matchQuery("screenName", screenName)) // Query
                    //                .setPostFilter(QueryBuilders.rangeQuery("age").from(12).to(18)) // Filter
                    .execute().actionGet();

            SearchHits hits = response.getHits();

            System.out.println("total hits: " + hits.totalHits());
            System.out.println("total hits array length: " + hits.getHits().length);
            if (hits.getHits().length == 0) {
                System.out.println("could not find screenName " + screenName + " in elastich search.");
                System.out.println("Please choose an another seed.");
                System.out.println("Exiting now.");
                System.exit(3);
            }

            for (SearchHit hit : hits.getHits()) {
                Map<String, Object> source = hit.getSource();
                Number temp = (Number) source.get("userId");
                Long userId = temp.longValue();
                System.out.println("userId found for seed "+ screenName+": " + userId);
                id2ScreenNames.put(String.valueOf(userId.longValue()), screenName);
                break;
            }
            i++;
        }
        return id2ScreenNames;

    }

}
