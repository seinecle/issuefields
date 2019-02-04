///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package net.clementlevallois.networkfromseedswithredis;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import static net.clementlevallois.networkfromseedswithredis.Controller.graph;
//import org.nd4j.linalg.api.ndarray.INDArray;
//import org.nd4j.linalg.factory.Nd4j;
//
///**
// *
// * @author LEVALLOIS
// */
//public class Tests {
//
//    /**
//     * @param args the command line arguments
//     */
//    public static void main(String[] args) {
//        INDArray matrix = Nd4j.zeros(2000, 2000);
//
//        Map<EdgeTemp,Double> collection = new ConcurrentHashMap();
//        EdgeTemp e1 = new EdgeTemp(1, 2, 3d);
//        collection.put(e1, 3d);
//        EdgeTemp e2 = new EdgeTemp(2, 1, 3d);
//
//        collection = Utilities.addOrMergeEdgeIntoCollection(collection, e2);
//
//        System.out.println("list size: " + collection.size());
//    }
//
//}
