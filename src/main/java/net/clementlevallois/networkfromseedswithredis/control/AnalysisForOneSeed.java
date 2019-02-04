/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.networkfromseedswithredis.control;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import net.clementlevallois.networkfromseedswithredis.ops.GetListsTooBigTooSmall;
import net.clementlevallois.networkfromseedswithredis.model.EdgeTempBase;
import net.clementlevallois.networkfromseedswithredis.db.RedisInitializer;
import net.clementlevallois.networkfromseedswithredis.db.ElasticSearchInitializer;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import net.clementlevallois.utils.Clock;
import net.clementlevallois.utils.MultisetMostFrequentFiltering;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

/**
 *
 * @author LEVALLOIS
 */
public class AnalysisForOneSeed {

//    WARNING: yes, corona one users must have at least k lists in common to be in the graph
//            but they can have just k = 1 list in common with the seed to be considered as candidates to the graph
//            the risk is that k= 1 is too low
//            mitigation: we take just the giant component of the graph formed at the end so unrelated nodes will be deleted.
    RedisInitializer redis;
    ElasticSearchInitializer es;
    Integer k;
    Boolean forceSeedInclusion;
    Integer maxUsersCoronaOne;
    Integer maxUsersCoronaTwo;

    Boolean limitNumberOfLists;
    Integer maxListMembers;
    Integer maxListsPerUsers;

    int counterLoops = 0;
    int loopsToDo = 0;

    public AnalysisForOneSeed(ElasticSearchInitializer es, RedisInitializer redis, int k, boolean limitNumberOfLists, int maxListMembers, int maxListsPerUsers, boolean f, int maxUsersCoronaOne, int maxUsersCoronaTwo) throws UnknownHostException, IOException {
        this.redis = redis;
        this.es = es;
        this.k = k;
        this.forceSeedInclusion = f;
        this.limitNumberOfLists = limitNumberOfLists;
        this.maxListMembers = maxListMembers;
        this.maxListsPerUsers = maxListsPerUsers;
        this.maxUsersCoronaOne = maxUsersCoronaOne;
        this.maxUsersCoronaTwo = maxUsersCoronaTwo;
    }

    public ResponseProcess getGraph(String seed) throws IOException {

        Clock clock = new Clock("get lists too big or too small");
        Set<String> listsTooBigOrTooSmall = GetListsTooBigTooSmall.getListsWithCardinalityTooHighOrTooSmall(redis, k, maxListMembers);
        clock.closeAndPrintClock();

        clock = new Clock("getting the lists to which the seed belongs");
        Set<String> listsOfSeed = getListsOfSeed(seed);
        clock.closeAndPrintClock();

        clock = new Clock("getting the users in lists to which the seed belongs");
        Set<String> usersInListsOfSeed = getUsersInListsOfSeed(listsOfSeed, listsTooBigOrTooSmall);
        clock.closeAndPrintClock();

        clock = new Clock("getting the lists of the users in lists to which the seed belongs");
        Map<String, Set<String>> usersInCoronaOneAndTheirLists = getListsOfCoUSersOfTheSeed(seed, k, listsTooBigOrTooSmall, listsOfSeed, usersInListsOfSeed);
        clock.closeAndPrintClock();

        clock = new Clock("getting the links between the users of the lists to which the seed belongs");
        ResponseMethodFindingLinks response = findLinksBetweenCoronaOneUsersBasedOnCommonLists(usersInCoronaOneAndTheirLists, seed);
        clock.closeAndPrintClock();

        clock = new Clock("checking if seed is included in corona one");
        isSeedInCoronaOne(response.getEdgesToLoadInGraph(), seed);
        clock.closeAndPrintClock();

        clock = new Clock("get set of lists of users corona one");
        Set<String> setOfListsOfUsersCoronaOne = getSetOfListsOfUsersCoronaOne(usersInCoronaOneAndTheirLists);
        clock.closeAndPrintClock();

        clock = new Clock("get users of lists of users corona one");
        Set<String> usersOfListsOfUsersOfCoronaOne = getUsersOfListsOfUsersOfCoronaOne(setOfListsOfUsersCoronaOne, listsTooBigOrTooSmall);
        clock.closeAndPrintClock();

        clock = new Clock("remove corona one users from the users of corona 2");
        Set<String> usersCoronaTwoWithoutCoronaOneUsers = removingUsersCoronaOneFromUsersCoronaTwo(usersOfListsOfUsersOfCoronaOne, usersInCoronaOneAndTheirLists);
        clock.closeAndPrintClock();

        clock = new Clock("get lists of users in corona 2");
        Map<String, Set<String>> listsOfUsersCoronaTwo = getListsOfUsersCoronaTwo(usersCoronaTwoWithoutCoronaOneUsers, listsTooBigOrTooSmall, listsOfSeed);
        clock.closeAndPrintClock();

        clock = new Clock("find links between corona 2 and corona 1 users. add these links to the list of links to create");
        response = findLinksBetweenCorona2UsersAndCoronaOneUsers(usersInCoronaOneAndTheirLists, listsOfUsersCoronaTwo, response.getEdgesToLoadInGraph());
        clock.closeAndPrintClock();
                
        clock = new Clock("remove bidirectional edges");
        Queue<EdgeTempBase> linksToCreate = removeBiDirectionalEdges(response.getEdgesToLoadInGraph());
        clock.closeAndPrintClock();

        clock = new Clock("get set of node Ids to create");
        Set<String> nodeIdsToLoadInGraph = getSetOfNodeIdsToCreate(linksToCreate);
        clock.closeAndPrintClock();

        clock = new Clock("create a mapping from nodeIds to map index");
        Map<String, Integer> mappingNodeIdToMatrixIndex = createMappingNodeIdToMatrixIndex(nodeIdsToLoadInGraph);
        clock.closeAndPrintClock();

        clock = new Clock("matrix operations");
        INDArray vectorOfIntensityValues = matrixOperations(seed, linksToCreate, mappingNodeIdToMatrixIndex);
        clock.closeAndPrintClock();

        ResponseProcess responseProcess = new ResponseProcess(seed, vectorOfIntensityValues, linksToCreate, mappingNodeIdToMatrixIndex, response.getListsUsedToInferLinks());

        return responseProcess;

    }

    private Set<String> getListsOfSeed(String seed) {

        final Set<String> listsToWhichTheSeedBelongs;
        final Set<String> listsToWhichTheSeedBelongsLimitedToMaxList;

        try (Jedis jedis = redis.getJedisPool().getResource()) {

            listsToWhichTheSeedBelongs = jedis.smembers("userId:" + seed);
            if (limitNumberOfLists) {
                listsToWhichTheSeedBelongsLimitedToMaxList = listsToWhichTheSeedBelongs.stream().limit(maxListsPerUsers).collect(Collectors.toSet());
            } else {
                listsToWhichTheSeedBelongsLimitedToMaxList = listsToWhichTheSeedBelongs;
            }
            System.out.println("number of lists of this seed: " + listsToWhichTheSeedBelongs.size());

        }

        return listsToWhichTheSeedBelongsLimitedToMaxList;

    }

    private Set<String> getUsersInListsOfSeed(Set<String> listsOfSeed, Set<String> listsTooBigOrTooSmall) {
        Set<String> setMembersInListsOfTheSeed = new HashSet();
        Multiset<String> multisetMembersInListsOfTheSeed = HashMultiset.create();

        try (Jedis jedis = redis.getJedisPool().getResource()) {

            for (Iterator<String> it = listsOfSeed.iterator(); it.hasNext();) {
                String list = "listId:" + it.next();
                Long listCardinality = jedis.scard(list);
                if (listCardinality < maxListMembers & listCardinality >= k) {
                    Set<String> membersOfList = jedis.smembers(list);
                    multisetMembersInListsOfTheSeed.addAll(membersOfList);
                }
            }
        }

        MultisetMostFrequentFiltering ranking = new MultisetMostFrequentFiltering();
        List<Multiset.Entry<String>> sortDesc = ranking.sortDesc(multisetMembersInListsOfTheSeed);

        for (Multiset.Entry<String> entry : sortDesc) {
            if (setMembersInListsOfTheSeed.size() < maxUsersCoronaOne) {
                setMembersInListsOfTheSeed.add(entry.getElement());
            }
        }

        System.out.println("number of users with lists in common with the seed: " + multisetMembersInListsOfTheSeed.size());
        System.out.println("number of users with with lists in common with the seed, taking those with most lists in common first and capping:  " + setMembersInListsOfTheSeed.size());

        return setMembersInListsOfTheSeed;

    }

    private Map<String, Set<String>> getListsOfCoUSersOfTheSeed(String seed, int k, Set<String> listsTooBigOrTooSmall, Set<String> listsOfSeed, Set<String> setMembersInListsOfTheSeed) {
        Map<String, Response<Set<String>>> responses = new HashMap();
        Map<String, Set<String>> usersCoronaOneAndTheirLists = new HashMap();

        try (Jedis jedis = redis.getJedisPool().getResource()) {

            Pipeline p = jedis.pipelined();

            for (Iterator<String> it = setMembersInListsOfTheSeed.iterator(); it.hasNext();) {
                String user = it.next();
                responses.put(user, p.smembers("userId:" + user));
                it.remove();
            }
            p.sync();

            responses.entrySet().forEach((entry) -> {
                //making sure to remove lists which are too big
                Set<String> listsOfCoUser = entry.getValue().get();

                //limiting the number of lists, as considering all of them would be computationally intensive
                // among the lists we keep, we keep first those which are common to the seed
                if (limitNumberOfLists) {
                    //taking all the lists of the co-user *which are common to the lists of the seed*
                    Set<String> listsOfCoUserInCommonToSeed = listsOfCoUser
                            .parallelStream()
                            .filter(l -> listsOfSeed.contains(l))
                            .limit(maxListsPerUsers)
                            .collect(Collectors.toSet());
                    //taking all the lists of the co-user *which are different from the lists of the seed*, up to a the limit set by maxListsPerUsers minus the number of lists in common with the seed
                    Set<String> listsOfCoUserDifferentFromSeed = listsOfCoUser
                            .parallelStream()
                            .filter(l -> {
                                return !listsOfSeed.contains(l);
                            })
                            .limit((maxListsPerUsers - listsOfCoUserInCommonToSeed.size()))
                            .collect(Collectors.toSet());
                    listsOfCoUser = new HashSet();
                    listsOfCoUser.addAll(listsOfCoUserInCommonToSeed);

                    listsOfCoUser.addAll(listsOfCoUserDifferentFromSeed);
                }

                Iterator<String> it = listsOfCoUser.iterator();
                while (it.hasNext()) {
                    String listId = "listId:" + it.next();
                    if (listsTooBigOrTooSmall.contains(listId)) {
                        it.remove();
                    }
                }
                if (!listsOfCoUser.isEmpty()) {
                    usersCoronaOneAndTheirLists.put(entry.getKey(), listsOfCoUser);
                }
            });
            usersCoronaOneAndTheirLists.put(seed, listsOfSeed);

        }

        System.out.println("users in corona one (\"usersCoronaOneAndTheirLists.keySet()\"): " + usersCoronaOneAndTheirLists.keySet().size());

        return usersCoronaOneAndTheirLists;
    }

    private ResponseMethodFindingLinks findLinksBetweenCoronaOneUsersBasedOnCommonLists(Map<String, Set<String>> usersCoronaOneAndTheirLists, String seed) {
        Queue<EdgeTempBase> edgesToLoadInGraph = new ConcurrentLinkedQueue();
        Multiset<String> listsUsed = HashMultiset.create();
        ResponseMethodFindingLinks response = new ResponseMethodFindingLinks();
        counterLoops = 0;

        Set<String> keySet = usersCoronaOneAndTheirLists.keySet();
        keySet.parallelStream().forEach(key1 -> {
            Set<String> lists1 = usersCoronaOneAndTheirLists.get(key1);
            counterLoops++;
            if (counterLoops % 100 == 0) {
                System.out.println("co-members left to examine: " + (keySet.size() - counterLoops));
            }
            keySet.stream().forEach(key2 -> {
                if (!key1.equals(key2)) {
                    Set<String> lists2 = usersCoronaOneAndTheirLists.get(key2);
                    Sets.SetView<String> intersection = Sets.intersection(lists1, lists2);
                    int listsInCommon = intersection.size();
                    // we want the seed to be in the network even if it has only a link > 1 to any other co-user 
                    if (listsInCommon >= k || ((forceSeedInclusion && (key1.equals(seed) || key2.equals(seed))))) {
                        if (key1 != null & key2 != null) {
                            edgesToLoadInGraph.add(new EdgeTempBase(key1, key2, (double) listsInCommon));
                            listsUsed.addAll(intersection);
                        }
                    }

                }
            });
        });
        response.setEdgesToLoadInGraph(edgesToLoadInGraph);
        response.setListsUsedToInferLinks(listsUsed);

        System.out.println("edges to create: " + edgesToLoadInGraph.size());
        return response;

    }

    private boolean isSeedInCoronaOne(Queue<EdgeTempBase> edgesToLoadInGraph, String seed) {

        boolean seedInCoronaOne = false;
        for (EdgeTempBase edge : edgesToLoadInGraph) {
            if (edge.getSource().equals(seed) | edge.getTarget().equals(seed)) {
                seedInCoronaOne = true;
            }
        }

        if (seedInCoronaOne) {
            System.out.println("seed in corona one");
        } else {
            System.out.println("seed not in corona one");
            System.out.println("There was no list co-members of the seed who had at least " + k + " lists in common with the seed");
            System.out.println("We can't proceed to corona 2");
            System.out.println("Please try another seed. Exiting now");
            System.exit(3);

        }
        return seedInCoronaOne;

    }

    private Set<String> getSetOfListsOfUsersCoronaOne(Map<String, Set<String>> usersCoronaOneAndTheirLists) {
        // getting a set of all lists of the users of Corona One
        Set<String> setOflistsOfUsersInCoronaOne = new HashSet();
        for (String user : usersCoronaOneAndTheirLists.keySet()) {
            setOflistsOfUsersInCoronaOne.addAll(usersCoronaOneAndTheirLists.get(user));
        }
        System.out.println("Users in Corona 1 belong to " + setOflistsOfUsersInCoronaOne.size() + " lists.");
        return setOflistsOfUsersInCoronaOne;
    }

    private Set<String> getUsersOfListsOfUsersOfCoronaOne(Set<String> setOflistsOfUsersInCoronaOne, Set<String> listsTooBigOrTooSmall) {
        Map<String, Response<Set<String>>> responses = new HashMap();
        Multiset<String> multisetMembersInListsOfCoronaOneUsers = HashMultiset.create();
        Set<String> setMembersInListsOfCoronaOneUsers = new HashSet();
        loopsToDo = setOflistsOfUsersInCoronaOne.size();
        counterLoops = 0;

        try (Jedis jedis = redis.getJedisPool().getResource()) {
            Iterator<String> iteratorOnlistsOfUsersCoronaOne = setOflistsOfUsersInCoronaOne.iterator();
            Pipeline p = jedis.pipelined();

            while (iteratorOnlistsOfUsersCoronaOne.hasNext()) {
                counterLoops++;
                if (counterLoops % 10_000 == 0) {
                    System.out.println("lists that need to get their users fetched in redis: " + (loopsToDo - counterLoops));
                }

                String listOfUserCoronaOne = iteratorOnlistsOfUsersCoronaOne.next();
                listOfUserCoronaOne = "listId:" + listOfUserCoronaOne;
                if (!listsTooBigOrTooSmall.contains(listOfUserCoronaOne)) {
                    responses.put(listOfUserCoronaOne, p.smembers(listOfUserCoronaOne));
                }

            }
            p.sync();
            responses.entrySet().forEach((entry) -> {
                multisetMembersInListsOfCoronaOneUsers.addAll(entry.getValue().get());
            });

            MultisetMostFrequentFiltering ranking = new MultisetMostFrequentFiltering();
            List<Multiset.Entry<String>> sortDesc = ranking.sortDesc(multisetMembersInListsOfCoronaOneUsers);

            for (Multiset.Entry<String> entry : sortDesc) {
                if (setMembersInListsOfCoronaOneUsers.size() < maxUsersCoronaTwo) {
                    setMembersInListsOfCoronaOneUsers.add(entry.getElement());
                }
            }

            System.out.println("number of users with lists in common with corona one users: " + multisetMembersInListsOfCoronaOneUsers.size());
            System.out.println("number of users with with lists in common with corona one users, taking those with most lists in common first and capping:  " + setMembersInListsOfCoronaOneUsers.size());

        }
        return setMembersInListsOfCoronaOneUsers;
    }

    private Set<String> removingUsersCoronaOneFromUsersCoronaTwo(Set<String> setMembersInListsOfCoronaOneUsers, Map<String, Set<String>> usersCoronaOneAndTheirLists) {
        Set<String> usersCorona2 = new HashSet();
        usersCorona2.addAll(setMembersInListsOfCoronaOneUsers);
        usersCorona2.removeAll(usersCoronaOneAndTheirLists.keySet());
        System.out.println("number of users in Corona 2 (once Corona 1 users removed): " + usersCorona2.size());
        return usersCorona2;

    }

    private Map<String, Set<String>> getListsOfUsersCoronaTwo(Set<String> usersCorona2, Set<String> listsTooBigOrTooSmall, Set<String> listsOfSeed) {
        Map<String, Response<Set<String>>> responses;

        Map<String, Set<String>> usersCorona2AndTheirLists = new HashMap();
        int batchSize = 5_000;
        int loopsInBatch = 0;
        counterLoops = 0;
        loopsToDo = usersCorona2.size();

        try (Jedis jedis = redis.getJedisPool().getResource()) {
            while (!usersCorona2.isEmpty()) {
                responses = new HashMap();
                Pipeline p;
                p = jedis.pipelined();
                String user;
                loopsInBatch = 0;
                Iterator<String> it2 = usersCorona2.iterator();
                while (it2.hasNext() & loopsInBatch < batchSize) {
                    counterLoops++;
                    loopsInBatch++;
                    if (counterLoops % 5_000 == 0) {
                        System.out.println("users that need their lists to be fetched in redis: " + (loopsToDo - counterLoops));
                    }
                    user = it2.next();
                    responses.put(user, p.smembers("userId:" + user));
                    it2.remove();
                }
                p.sync();

                responses.entrySet().forEach((entry) -> {
                    //making sure to remove lists which are too big
                    Set<String> listsOfUserCorona2 = entry.getValue().get();
                    if (limitNumberOfLists) {
                        listsOfUserCorona2 = listsOfUserCorona2.stream().limit(maxListsPerUsers).collect(Collectors.toSet());

                        Set<String> listsOfCoUserInCommonToSeed = listsOfUserCorona2.parallelStream().filter(l -> listsOfSeed.contains(l)).limit(maxListsPerUsers).collect(Collectors.toSet());
                        Set<String> listsOfCoUserDifferentFromSeed = listsOfUserCorona2.parallelStream().filter(l -> {
                            return !listsOfSeed.contains(l);
                        }).limit((maxListsPerUsers - listsOfCoUserInCommonToSeed.size())).collect(Collectors.toSet());
                        listsOfUserCorona2 = new HashSet();
                        listsOfUserCorona2.addAll(listsOfCoUserInCommonToSeed);
                        listsOfUserCorona2.addAll(listsOfCoUserDifferentFromSeed);

                    }

                    Iterator<String> it = listsOfUserCorona2.iterator();
                    while (it.hasNext()) {
                        String listId = "listId:" + it.next();
                        if (listsTooBigOrTooSmall.contains(listId)) {
                            it.remove();
                        }
                    }
                    if (!listsOfUserCorona2.isEmpty()) {
                        usersCorona2AndTheirLists.put(entry.getKey(), listsOfUserCorona2);
                    }
                });
            }
        }

        return usersCorona2AndTheirLists;

    }

    private ResponseMethodFindingLinks findLinksBetweenCorona2UsersAndCoronaOneUsers(Map<String, Set<String>> usersCoronaOneAndTheirLists, Map<String, Set<String>> listsOfUsersCoronaTwo, Queue<EdgeTempBase> linksToCreate) {

        Set<String> usersCorona1 = usersCoronaOneAndTheirLists.keySet();
        Set<String> usersCorona2 = listsOfUsersCoronaTwo.keySet();
        counterLoops = 0;
        loopsToDo = usersCorona2.size();

        Multiset<String> listsUsed = HashMultiset.create();
        ResponseMethodFindingLinks response = new ResponseMethodFindingLinks();

        usersCorona2.parallelStream().forEach(aUserOfCorona2 -> {
            counterLoops++;
            if (counterLoops % 10_000 == 0) {
                System.out.println("users left to examine for links: " + (loopsToDo - counterLoops));
            }

            Set<String> listsOfAUserOfCorona2 = listsOfUsersCoronaTwo.get(aUserOfCorona2);
            usersCorona1.parallelStream().forEach(aUserOfCoronaOne -> {

                if (!aUserOfCorona2.equals(aUserOfCoronaOne)) {
                    Set<String> listsOfAUserOfCoronaOne = usersCoronaOneAndTheirLists.get(aUserOfCoronaOne);
                    Set<String> intersection = Sets.intersection(listsOfAUserOfCoronaOne, listsOfAUserOfCorona2);
                    if (intersection.size() >= k) {
                        if (aUserOfCorona2 != null & aUserOfCoronaOne != null) {
                            linksToCreate.add(new EdgeTempBase(aUserOfCorona2, aUserOfCoronaOne, (double) intersection.size()));
                            listsUsed.addAll(intersection);
                        }
                    }
                }
            });
        });
        response.setEdgesToLoadInGraph(linksToCreate);
        response.setListsUsedToInferLinks(listsUsed);
        System.out.println("number of edges to create (with duplicates): " + linksToCreate.size());

        return response;

    }

    private Queue<EdgeTempBase> removeBiDirectionalEdges(Queue<EdgeTempBase> linksToCreate) {
        Map<String, Integer> nodeDegree = new HashMap();
        Set<String> edgeLinks = new HashSet();
        Queue<EdgeTempBase> linksToCreateWithoutIsolatedPairs = new ConcurrentLinkedQueue();
        System.out.println("");
        System.out.println("number of edges before the filtering: " + linksToCreate.size());

        for (EdgeTempBase e : linksToCreate) {
            if (e.getSource() == null | e.getTarget() == null) {
                System.out.println("e is null");
                System.out.println(e);
            }

            if (nodeDegree.containsKey(e.getSource())) {
                nodeDegree.put(e.getSource(), nodeDegree.get(e.getSource()) + 1);
            } else {
                nodeDegree.put(e.getSource(), 1);
            }
            if (nodeDegree.containsKey(e.getTarget())) {
                nodeDegree.put(e.getTarget(), nodeDegree.get(e.getTarget()) + 1);
            } else {
                nodeDegree.put(e.getTarget(), 1);
            }
            // adding the edges to a set - but just edges in one direction
            if (!edgeLinks.contains(e.getTarget() + "|" + e.getSource())) {
                edgeLinks.add(e.getSource() + "|" + e.getTarget());
            }
        }

        for (EdgeTempBase e : linksToCreate) {
            if (!(nodeDegree.get(e.getSource()) == 1 & nodeDegree.get(e.getTarget()) == 1)) {
                if (edgeLinks.contains(e.getSource() + "|" + e.getTarget())) {
                    linksToCreateWithoutIsolatedPairs.add(e);
                }
            }
        }
        System.out.println("number of edges after the filtering: " + linksToCreateWithoutIsolatedPairs.size());

        return linksToCreateWithoutIsolatedPairs;
    }

    private Set<String> getSetOfNodeIdsToCreate(Queue<EdgeTempBase> linksToCreate) {
        Set<String> nodeIdsToLoadInTheGraph = new HashSet();

        for (EdgeTempBase e : linksToCreate) {
            if (e.getSource() == null | e.getTarget() == null) {
                System.out.println("e is null");
                System.out.println(e);
            }
            nodeIdsToLoadInTheGraph.add(e.getSource());
            nodeIdsToLoadInTheGraph.add(e.getTarget());
        }

        System.out.println("nodes to load in the graph according to nodeIdsToLoadInTheGraph.size(): " + nodeIdsToLoadInTheGraph.size());

        return nodeIdsToLoadInTheGraph;
    }

    private Map<String, Integer> createMappingNodeIdToMatrixIndex(Set<String> nodeIdsToLoadInTheGraph) {
        //crée un mapping de node id vers node index dans la matrix

        Map<String, Integer> mapNodeIdToMatrixIndex = new HashMap();
        Iterator<String> itNodes = nodeIdsToLoadInTheGraph.iterator();
        int indexCounter = 0;
        while (itNodes.hasNext()) {
            String next = itNodes.next();
            mapNodeIdToMatrixIndex.put(next, indexCounter);
            indexCounter++;
        }
        System.out.println("size of mapNodeIdToMatrixIndex: " + mapNodeIdToMatrixIndex.size());

        return mapNodeIdToMatrixIndex;

    }

    private INDArray matrixOperations(String seed, Queue<EdgeTempBase> linksToCreate, Map<String, Integer> mapNodeIdToMatrixIndex) {
        INDArray sumVectors;

        INDArray matrix = Nd4j.zeros(mapNodeIdToMatrixIndex.size(), mapNodeIdToMatrixIndex.size());

        //remplir les cells avec le weight de chacun des edges
        for (EdgeTempBase e : linksToCreate) {
            int row = mapNodeIdToMatrixIndex.get(e.getSource());
            int col = mapNodeIdToMatrixIndex.get(e.getTarget());
            int[] indexA = {row, col};
            int[] indexB = {col, row};
            double weightA = matrix.getDouble(row, col);
            double weightB = matrix.getDouble(col, row);
            matrix.putScalar(indexA, weightA + e.getWeight());
            matrix.putScalar(indexB, weightB + e.getWeight());
        }

        // former le vecteur qui correspond à la seed
        int seedIndex = mapNodeIdToMatrixIndex.get(seed);
        INDArray seedVector = matrix.getColumn(seedIndex);

        // multiplier la matrice par le vecteur, ca retourne un vecteur
        INDArray v1 = matrix.mmul(seedVector);

        // matrice x vecteur où il y a que des 1
        INDArray ones = Nd4j.ones(mapNodeIdToMatrixIndex.size(), 1);
        INDArray v2 = matrix.mmul(ones);

        // somme des deux vecteurs. Ce sera not data structure finale pour écrire l'inensité score des nodes
        sumVectors = v1.add(v2);

        // calculer la moyenne de ce vecteur
        Number meanVector = sumVectors.ameanNumber();

        // calculer l'écart type de ce vecteur
        Number standardDeviation = sumVectors.stdNumber();

        // pour chaque element du vecteur, je soustrais la moyenne et je divise par l'écart type
        sumVectors = sumVectors.sub(meanVector);
        sumVectors = sumVectors.div(standardDeviation);

        // c'est l'intensité de chaque node.
        // si la seed a un score < 0, mettre un score légèrement positif.
        if (sumVectors.getDouble(seedIndex) < 0) {
            sumVectors.putScalar(seedIndex, 0.1);
        }
        System.out.println("size of sumVectors: " + sumVectors.length());

        return sumVectors;

    }


}
