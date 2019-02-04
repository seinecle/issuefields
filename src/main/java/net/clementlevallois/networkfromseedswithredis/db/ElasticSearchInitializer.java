/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.networkfromseedswithredis.db;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

/**
 *
 * @author LEVALLOIS
 */
public class ElasticSearchInitializer {

    private TransportClient client;
    private ObjectMapper mapper;

    public void initElasticSearch() throws UnknownHostException {

        //elasticSearch
        Settings settings = Settings.builder().put("client.transport.ping_timeout", "30s").build();
        client = new PreBuiltTransportClient(settings)
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300));

        // instance a json mapper
        mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // create once, reuse

    }

    public TransportClient getClient() {
        return client;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }
    
    

}
