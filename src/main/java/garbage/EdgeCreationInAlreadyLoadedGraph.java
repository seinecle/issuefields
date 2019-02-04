///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package net.clementlevallois.networkfromseedswithredis;
//
//import org.gephi.graph.api.Edge;
//import org.gephi.graph.api.Graph;
//import org.gephi.graph.api.GraphModel;
//import org.gephi.graph.api.Node;
//
///**
// *
// * @author LEVALLOIS
// */
//public class EdgeCreationInAlreadyLoadedGraph {
//
//    GraphModel graphModel;
//    Graph graph;
//
//    public EdgeCreationInAlreadyLoadedGraph(GraphModel graphModel, Graph graph) {
//        this.graphModel = graphModel;
//        this.graph = graph;
//    }
//
//    public void addEdge(String node1Id, String node2Id, double weight) {
//
//        Node node1 = graph.getNode((String) node1Id);
//        Node node2 = graph.getNode((String) node2Id);
//
//        if (node1 == null) {
//            node1 = graphModel.factory().newNode(node1Id);
//            graph.addNode(node1);
//        }
//        if (node2 == null) {
//            node2 = graphModel.factory().newNode(node2Id);
//            graph.addNode(node2);
//        }
//
//        Edge edge = graph.getEdge(node1, node2);
//
//        if (edge != null) {
//            graph.removeEdge(edge);
//            double prevWeight = edge.getWeight();
//            edge.setWeight(prevWeight + weight);
//            graph.addEdge(edge);
//        } else {
//            Edge newEdge = graphModel.factory().newEdge(node1, node2, 0, (double) weight, false);
//            graph.addEdge(newEdge);
//        }
//
//    }
//
//    public Graph getGraph() {
//        return graph;
//    }
//    
//    
//
//}
