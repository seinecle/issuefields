/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.networkfromseedswithredis.control;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import net.clementlevallois.networkfromseedswithredis.ops.ScreennameToTwitterIdConverter;
import net.clementlevallois.networkfromseedswithredis.db.RedisInitializer;
import net.clementlevallois.networkfromseedswithredis.db.ElasticSearchInitializer;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import net.clementlevallois.networkfromseedswithredis.io.PrintLists;
import net.clementlevallois.utils.Clock;
import net.clementlevallois.networkfromseedswithredis.picocli.CommandLine;
import net.clementlevallois.networkfromseedswithredis.picocli.CommandLine.Command;
import net.clementlevallois.networkfromseedswithredis.picocli.CommandLine.Option;
import net.clementlevallois.networkfromseedswithredis.picocli.CommandLine.Parameters;

/**
 *
 * @author LEVALLOIS
 */
@Command(description = "Analyzes seeds (Twitter user accounts) to reconstruct an issue field",
        name = "issue field builder", mixinStandardHelpOptions = true, version = "1.0")
public class Controller implements Callable {

    /**
     * @param args the command line arguments
     */
    @Parameters(paramLabel = "seeds", description = "one seed or more to analyze. Ex: @Francois_Ruffin")
    String[] seedsNames = {"@rendemes"};

    @Option(names = {"-k", "--minListsInCommon"}, description = "min # of lists in common between 2 users to create a link. (default: ${DEFAULT-VALUE})")
    int k = 5;

    @Option(names = {"-fsi", "--forceSeedInclusion"}, description = "force the inclusion of the seed in the graph, even if it has not any link of strength k with any co-member (default: ${DEFAULT-VALUE})")
    boolean forceSeedInclusion = true;

    @Option(names = {"-uni", "--unique"}, description = "Creates one gexf per seed (default: ${DEFAULT-VALUE})")
    boolean unique = true;

    boolean limitNumberOfLists = true;

    @Option(names = {"-m", "--maxListMembers"}, description = "lists with # of members of this param will not be considered. (default: ${DEFAULT-VALUE})")
    int maxListMembers = 500;

    @Option(names = {"-c1", "--maxUsersCoronaOne"}, description = "max number of users for corona one. (default: ${DEFAULT-VALUE})")
    int maxUsersCoronaOne = 10_000;

    @Option(names = {"-c2", "--maxUsersCoronaTwo"}, description = "max number of users for corona two. (default: ${DEFAULT-VALUE})")
    int maxUsersCoronaTwo = 100_000;

    @Option(names = {"-l", "--maxListsPerUser"}, description = "max number of lists to be analized per user Bigger numbers take much more time. (default: ${DEFAULT-VALUE})")
    int maxListsPerUser = 100;

    public static void main(String[] args) throws UnknownHostException, IOException {
        CommandLine.call(new Controller(), args);
    }

    @Override
    public Void call() throws Exception {

        Clock clockOverall = new Clock("overall process");

        System.out.println("max lists per user: " + maxListsPerUser);
        System.out.println("min lists for a connection (k): " + k);
        System.out.println("unique " + unique);

        Clock clock = new Clock("initializing the redis connection");
        RedisInitializer redis = new RedisInitializer();
        redis.initRedis();
        clock.closeAndPrintClock();

        clock = new Clock("initializing the elastich search connection");
        ElasticSearchInitializer es = new ElasticSearchInitializer();
        es.initElasticSearch();
        clock.closeAndPrintClock();

        clock = new Clock("get seeds(s) twitter id from their screen names");
        ScreennameToTwitterIdConverter converter = new ScreennameToTwitterIdConverter(seedsNames, es.getClient(), es.getMapper());
        Map<String, String> id2ScreenNames = converter.searchScreenNameReturnsUserId(seedsNames);
        clock.closeAndPrintClock();

        List<ResponseProcess> responses = new ArrayList();

        AnalysisForOneSeed graphFromSeed = new AnalysisForOneSeed(es, redis, k, limitNumberOfLists, maxListMembers, maxListsPerUser, forceSeedInclusion, maxUsersCoronaOne, maxUsersCoronaTwo);
        for (String seed : id2ScreenNames.keySet()) {
            clock = new Clock("analyze seed " + seed);
            ResponseProcess responseForOneSeed = graphFromSeed.getGraph(seed);
            responses.add(responseForOneSeed);
            clock.closeAndPrintClock();
            if (unique) {
                clock = new Clock("graph creation for one seed");
                List<ResponseProcess> uniqueResponse = new ArrayList();
                uniqueResponse.add(responseForOneSeed);
                GraphCreationAndAggregation graphMaker = new GraphCreationAndAggregation(uniqueResponse, es, k, maxListsPerUser);
                graphMaker.proceedToGraphCreationAndAggregation(unique, id2ScreenNames);
                clock.closeAndPrintClock();
                clock = new Clock("print lists used for this seed");
                PrintLists printLists = new PrintLists();
                printLists.printLists(responseForOneSeed.getListsUsedToCreateLinks(), seed);
                clock.closeAndPrintClock();

            }
        }

        clock = new Clock("aggregation of results per seed and graph creation");
        GraphCreationAndAggregation graphMaker = new GraphCreationAndAggregation(responses, es, k, maxListsPerUser);
        graphMaker.proceedToGraphCreationAndAggregation(unique, id2ScreenNames);
        clock.closeAndPrintClock();

        clock = new Clock("print lists used for all seeds");
        Multiset<String> allLists = HashMultiset.create();
        for (ResponseProcess responseProcess: responses){
            allLists.addAll(responseProcess.getListsUsedToCreateLinks());
        }        
        PrintLists printLists = new PrintLists();
        printLists.printLists(allLists, "all seeds");
        clock.closeAndPrintClock();

        clockOverall.closeAndPrintClock();

        return null;
    }

}
