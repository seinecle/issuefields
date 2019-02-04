/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.networkfromseedswithredis.io;

import com.google.common.collect.Multiset;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import net.clementlevallois.utils.MultisetMostFrequentFiltering;

/**
 *
 * @author LEVALLOIS
 */
public class PrintLists {

    public void printLists(Multiset<String> listsUsedToCreateLinks, String seed) throws IOException {

        MultisetMostFrequentFiltering filter = new MultisetMostFrequentFiltering();
        List<Multiset.Entry<String>> sortDesc = filter.sortDesckeepAboveMinFreq(listsUsedToCreateLinks, 2);

        StringBuilder sb = new StringBuilder();
        for (Multiset.Entry<String> entry : sortDesc) {
            sb.append(entry.toString());
            sb.append("\n");
        }
        BufferedWriter newBufferedWriter = Files.newBufferedWriter(Paths.get("most frequent lists for " + seed + ".txt"), Charset.forName("UTF-8"), StandardOpenOption.CREATE);
        newBufferedWriter.write(sb.toString());
        newBufferedWriter.close();
    }

}
