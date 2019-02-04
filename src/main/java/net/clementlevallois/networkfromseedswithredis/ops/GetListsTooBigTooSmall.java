/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.networkfromseedswithredis.ops;

import net.clementlevallois.networkfromseedswithredis.db.RedisInitializer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.ScanParams;
import static redis.clients.jedis.ScanParams.SCAN_POINTER_START;
import redis.clients.jedis.ScanResult;

/**
 *
 * @author LEVALLOIS
 */
public class GetListsTooBigTooSmall {

    public static Set<String> getListsWithCardinalityTooHighOrTooSmall(RedisInitializer redis, int k, int maxListMembers) throws IOException {
        Set<String> listsTooBigOrTooSmall = new HashSet();
        Map<String, Response<Long>> responsesCard = new HashMap();

        Path listsPath = null;
        if (Files.exists(Paths.get("lists too big or too small_" + k + ".txt"))) {
            listsPath = Paths.get("lists too big or too small_" + k + ".txt");
        }
        if (Files.exists(Paths.get("../lists too big or too small_" + k + ".txt"))) {
            listsPath = Paths.get("lists too big or too small_" + k + ".txt");
        }
        if (listsPath != null) {
            System.out.println("file of lists too big or too small found for k = " + k);
            BufferedReader newBufferedReader = Files.newBufferedReader(listsPath);
            Stream<String> lines = newBufferedReader.lines();
            lines.forEach(l -> listsTooBigOrTooSmall.add(l));
            newBufferedReader.close();
        } else {
            System.out.println("file of lists too big or too small NOT found for k = " + k);

            try (Jedis jedis = redis.getJedisPool().getResource()) {
                ScanParams scanParams = new ScanParams().count(1000_000).match("listId:*");
                String cur = SCAN_POINTER_START;
                int counter = 0;
                Set<String> listKeys = new HashSet();
                do {
                    ScanResult<String> scanResult = jedis.scan(cur, scanParams);

                    // work with result
                    listKeys.addAll(scanResult.getResult());
                    cur = scanResult.getStringCursor();
                    System.out.println("looped " + counter++);
                } while (!cur.equals(SCAN_POINTER_START));

                Pipeline p = jedis.pipelined();

                listKeys.forEach((listKey) -> {
                    responsesCard.put(listKey, p.scard(listKey));
                });
                p.sync();

                System.out.println("finished looping through lists on redis. Now adding the correct ones to the list.");

                BufferedWriter newBufferedWriter = Files.newBufferedWriter(Paths.get("lists too big or too small_" + k + ".txt"));
                StringBuilder sb = new StringBuilder();
                responsesCard.entrySet().forEach((entry) -> {
                    long card = entry.getValue().get();
                    if (card > maxListMembers | card < k) {
                        listsTooBigOrTooSmall.add(entry.getKey());
                        sb.append(entry.getKey()).append("\n");
                    }
                });
                newBufferedWriter.write(sb.toString());
                newBufferedWriter.close();
            }

        }

        System.out.println("number of lists too big or too small to be considered: " + listsTooBigOrTooSmall.size());
        return listsTooBigOrTooSmall;
    }

}
