/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.networkfromseedswithredis.control;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import net.clementlevallois.networkfromseedswithredis.model.EdgeTempBase;
import org.nd4j.linalg.api.ndarray.INDArray;

/**
 *
 * @author LEVALLOIS
 */
public class ResponseProcess {

    INDArray vectorOfIntensityValues;
    Queue<EdgeTempBase> linksToCreate;
    Map<String, Integer> mapNodeIdToMatrixIndex = new HashMap();
    String seed;
    Multiset<String> listsUsedToCreateLinks;

    public ResponseProcess(String seed,INDArray vectorOfIntensityValues, Queue<EdgeTempBase> linksToCreate, Map<String, Integer> mapNodeIdToMatrixIndex, Multiset<String> listsUsedToCreateLinks) {
        this.vectorOfIntensityValues = vectorOfIntensityValues;
        this.linksToCreate = linksToCreate;
        this.mapNodeIdToMatrixIndex = mapNodeIdToMatrixIndex;
        this.seed = seed;
        this.listsUsedToCreateLinks = listsUsedToCreateLinks;
    }

    public INDArray getVectorOfIntensityValues() {
        return vectorOfIntensityValues;
    }

    public Queue<EdgeTempBase> getLinksToCreate() {
        return linksToCreate;
    }

    public Map<String, Integer> getMapNodeIdToMatrixIndex() {
        return mapNodeIdToMatrixIndex;
    }
    
    public String getSeed(){
        return seed;
    }

    public Multiset<String> getListsUsedToCreateLinks() {
        return listsUsedToCreateLinks;
    }
    
    

}
