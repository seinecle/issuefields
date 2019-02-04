/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.networkfromseedswithredis.control;

import com.google.common.collect.Multiset;
import java.util.Queue;
import net.clementlevallois.networkfromseedswithredis.model.EdgeTempBase;

/**
 *
 * @author LEVALLOIS
 */
public class ResponseMethodFindingLinks {
    
    private Queue<EdgeTempBase> edgesToLoadInGraph;
    private Multiset<String> listsUsedToInferLinks;

    public ResponseMethodFindingLinks() {
    }

    public Queue<EdgeTempBase> getEdgesToLoadInGraph() {
        return edgesToLoadInGraph;
    }

    public void setEdgesToLoadInGraph(Queue<EdgeTempBase> edgesToLoadInGraph) {
        this.edgesToLoadInGraph = edgesToLoadInGraph;
    }

    public Multiset<String> getListsUsedToInferLinks() {
        return listsUsedToInferLinks;
    }

    public void setListsUsedToInferLinks(Multiset<String> listsUsedToInferLinks) {
        this.listsUsedToInferLinks = listsUsedToInferLinks;
    }
    
    
    
}
