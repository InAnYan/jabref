package org.jabref.logic.ai.util;

import java.util.Comparator;
import java.util.List;

import org.jabref.model.entry.BibEntry;

public class BibEntryListComparatorById implements Comparator<List<BibEntry>> {
    @Override
    public int compare(List<BibEntry> a, List<BibEntry> b) {
        if (a.size() != b.size()) {
            return Integer.compare(a.size(), b.size());
        }

        for (int i = 0; i < a.size(); i++) {
            int cmp = a.get(i).getId().compareTo(b.get(i).getId());
            if (cmp != 0) {
                return cmp;
            }
        }

        return 0;
    }
}
