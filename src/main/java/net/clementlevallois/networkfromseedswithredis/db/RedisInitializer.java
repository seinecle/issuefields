/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.networkfromseedswithredis.db;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.UnknownHostException;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 *
 * @author LEVALLOIS
 */
public class RedisInitializer {

    private ObjectMapper mapper;
    private JedisPool jedisPool;


    public void initRedis() throws UnknownHostException {

        // instance a json mapper
        mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // create once, reuse

        //jedis
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        jedisPool = new JedisPool(poolConfig, "localhost", 6379, 120_000);
    }

    public JedisPool getJedisPool() {
        return jedisPool;
    }    
    
    public ObjectMapper getMapper() {
        return mapper;
    }
    
    

}
