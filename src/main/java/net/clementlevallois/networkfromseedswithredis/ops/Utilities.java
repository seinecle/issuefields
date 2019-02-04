package net.clementlevallois.networkfromseedswithredis.ops;

///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package net.clementlevallois.networkfromseedswithredis;
//
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
///**
// *
// * @author LEVALLOIS
// */
//public class Utilities {
//
//    public static Map<EdgeTemp, Double> addOrMergeEdgeIntoCollection(Map<EdgeTemp, Double> edgesToLoadInGraph, EdgeTemp edgeTemp) {
//        if (edgeTemp == null) {
//            return edgesToLoadInGraph;
//        }
//        if (edgeTemp.getSet().size() < 2 | edgeTemp.getWeight() == 0d) {
//            return edgesToLoadInGraph;
//        }
//
//        Set<EdgeTemp> keySet = edgesToLoadInGraph.keySet();
//
//        if (keySet.contains(edgeTemp)) {
//            Iterator<EdgeTemp> it = keySet.iterator();
//            while (it.hasNext()) {
//                EdgeTemp next = it.next();
//                if (next.equals(edgeTemp)) {
//                    edgeTemp.setWeight(next.getWeight() + edgeTemp.getWeight());
//                    break;
//                }
//            }
//            edgesToLoadInGraph.put(edgeTemp, edgeTemp.getWeight());
//        } else {
//            edgesToLoadInGraph.put(edgeTemp, edgeTemp.getWeight());
//        }
//        return edgesToLoadInGraph;
//    }
//
//    public static String getUndirectedEdgeId(String a, String b) {
//        if (a.compareTo(b) < 0) {
//            return a + "-" + b;
//        } else {
//            return b + "-" + a;
//        }
//    }
//
//}
