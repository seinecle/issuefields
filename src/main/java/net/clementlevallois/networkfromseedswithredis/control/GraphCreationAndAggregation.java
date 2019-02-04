/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.networkfromseedswithredis.control;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import net.clementlevallois.networkfromseedswithredis.db.ElasticSearchInitializer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import net.clementlevallois.networkfromseedswithredis.model.EdgeTempBase;
import net.clementlevallois.datamining.graph.GraphEnrichment;
import net.clementlevallois.datamining.graph.GraphOperations;
import net.clementlevallois.utils.Clock;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.Node;
import org.gephi.io.importer.api.NodeDraft;
import org.nd4j.linalg.api.ndarray.INDArray;

/**
 *
 * @author LEVALLOIS
 */
public class GraphCreationAndAggregation {

    List<ResponseProcess> responses;
    GraphOperations graphOps;
    ElasticSearchInitializer es;
    int k;
    int maxListsPerUser;

    public GraphCreationAndAggregation(List<ResponseProcess> responses, ElasticSearchInitializer es, int k, int maxListsPerUser) {
        this.responses = responses;
        this.es = es;
        this.k = k;
        this.maxListsPerUser = maxListsPerUser;
    }

    public void proceedToGraphCreationAndAggregation(boolean unique, Map<String, String> id2ScreenNames) throws IOException {

        Clock clock = new Clock("initializing new graph");
        graphOps = new GraphOperations();
        graphOps.initANewGraph();
        clock.closeAndPrintClock();

        clock = new Clock("getting set of nodeIds to create, recording their intensity values");
        Map<String, Double> nodeIdsToLoadInTheGraph = getSetOfNodeIdsToCreate(responses);
        clock.closeAndPrintClock();

        clock = new Clock("adding these nodes and the edges to the gephi graph");
        graphCreation(graphOps, nodeIdsToLoadInTheGraph, responses);
        clock.closeAndPrintClock();

        clock = new Clock("loading graph");
        graphOps.loadGraph(false);
        clock.closeAndPrintClock();

        clock = new Clock("adding intensity values as attribute to the nodes");
        intensityScoreAddition(graphOps, nodeIdsToLoadInTheGraph);
        clock.closeAndPrintClock();

        clock = new Clock("enriching graph with attributes from elastic search");
        enrichGraphWithUserData(graphOps, es);
        clock.closeAndPrintClock();

        clock = new Clock("saving the gexf file to disk");
        saveToGexf(graphOps, responses, k, maxListsPerUser, id2ScreenNames);
        clock.closeAndPrintClock();

        if (unique) {
            clock = new Clock("create statistics report on the gexf file");
            createReport(graphOps, responses, id2ScreenNames);
            clock.closeAndPrintClock();
        }
    }

    private Map<String, Double> getSetOfNodeIdsToCreate(List<ResponseProcess> responses) {
        Map<String, Double> nodeIdsToLoadInTheGraph = new HashMap();
        Map<String, Integer> mapNodeIdToMatrixIndex;
        INDArray vectorOfIntensityValues;
        Queue<EdgeTempBase> linksToCreate;

        for (ResponseProcess response : responses) {
            System.out.println("getting set of node ids for seed " + response.getSeed());
            mapNodeIdToMatrixIndex = response.getMapNodeIdToMatrixIndex();
            System.out.println("size of mapNodeIdToMatrixIndex: " + mapNodeIdToMatrixIndex.size());

            vectorOfIntensityValues = response.getVectorOfIntensityValues();
            System.out.println("size of vectorOfIntensityValues: " + vectorOfIntensityValues.length());

            linksToCreate = response.getLinksToCreate();

            for (EdgeTempBase e : linksToCreate) {
                if (e.getSource() == null | e.getTarget() == null) {
                    System.out.println("e is null");
                    System.out.println(e);
                }
                //source
                int nodeMatrixIndex = mapNodeIdToMatrixIndex.get(e.getSource());
                double nodeIntensity = vectorOfIntensityValues.getDouble(nodeMatrixIndex);
                if (nodeIntensity > 0) {
                    if (nodeIdsToLoadInTheGraph.containsKey(e.getSource())) {
                        nodeIdsToLoadInTheGraph.put(e.getSource(), nodeIdsToLoadInTheGraph.get(e.getSource()) + nodeIntensity);
                    } else {
                        nodeIdsToLoadInTheGraph.put(e.getSource(), nodeIntensity);
                    }
                }
                //target
                nodeMatrixIndex = mapNodeIdToMatrixIndex.get(e.getTarget());
                nodeIntensity = vectorOfIntensityValues.getDouble(nodeMatrixIndex);
                if (nodeIntensity > 0) {
                    if (nodeIdsToLoadInTheGraph.containsKey(e.getTarget())) {
                        nodeIdsToLoadInTheGraph.put(e.getTarget(), nodeIdsToLoadInTheGraph.get(e.getTarget()) + nodeIntensity);
                    } else {
                        nodeIdsToLoadInTheGraph.put(e.getTarget(), nodeIntensity);
                    }
                }
            }
        }
        System.out.println("nodes to add in the graph: " + nodeIdsToLoadInTheGraph.size());
        return nodeIdsToLoadInTheGraph;
    }

    private void graphCreation(GraphOperations graphOps, Map<String, Double> nodeIdsToLoadInTheGraph, List<ResponseProcess> responses) {

        System.out.println("Adding nodes to the graph");

        for (Map.Entry<String, Double> nodeIdAndIntensity : nodeIdsToLoadInTheGraph.entrySet()) {
            NodeDraft node = graphOps.containerLoader.factory().newNodeDraft(nodeIdAndIntensity.getKey().replace("userId:", ""));
            node.setSize(10f);
            node.setLabel(nodeIdAndIntensity.getKey());
            graphOps.containerLoader.addNode(node);
        }

        System.out.println("Adding edges to the graph");
        for (ResponseProcess response : responses) {
            for (EdgeTempBase e : response.getLinksToCreate()) {
                graphOps.createEdge(e.getSource(), e.getTarget(), e.getWeight(), true);
            }
        }
    }

    private void intensityScoreAddition(GraphOperations graphOps, Map<String, Double> nodeIdsToLoadInTheGraph) {
        graphOps.getGm().getNodeTable().addColumn("intensity score", Double.class);
        Graph graph = graphOps.getGm().getGraph();
        for (Node node : graph.getNodes().toArray()) {
            node.setAttribute("intensity score", nodeIdsToLoadInTheGraph.get(node.getLabel()));
        }
    }

    private void enrichGraphWithUserData(GraphOperations graphOperations, ElasticSearchInitializer es) {
        GraphEnrichment graphEnrichment = new GraphEnrichment(graphOperations);
        graphEnrichment.enrichGraphWithUserInfo(es.getClient(), es.getMapper());

    }

    private void saveToGexf(GraphOperations graphOps, List<ResponseProcess> responses, int k, int maxListsPerUser,Map<String, String> id2ScreenNames) {
        StringBuilder sb = new StringBuilder();
        for (ResponseProcess res : responses) {
            sb.append(id2ScreenNames.get(res.getSeed())).append("_");
        }
        sb.append("minlists_");
        sb.append(k);
        sb.append("_maxlistspermember_ ");
        sb.append(maxListsPerUser);
        sb.append(".gexf");

        graphOps.exportToGexfFile(sb.toString());
    }

    private void createReport(GraphOperations graphOps, List<ResponseProcess> responses,Map<String, String> id2ScreenNames) throws IOException {
        String fileName = "report_" + id2ScreenNames.get(responses.get(0).getSeed()) + ".txt";
        StringBuilder sb = new StringBuilder();
        sb.append("Edge count: ").append(graphOps.getGm().getGraph().getEdgeCount());
        sb.append("\n");
        sb.append("Node count: ").append(graphOps.getGm().getGraph().getNodeCount());
        sb.append("\n");

        try (BufferedWriter newBufferedWriter = Files.newBufferedWriter(Paths.get(fileName), StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
            newBufferedWriter.write(sb.toString());
        }
    }

}
