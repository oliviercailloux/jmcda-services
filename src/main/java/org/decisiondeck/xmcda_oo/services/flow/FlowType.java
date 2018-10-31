package org.decisiondeck.xmcda_oo.services.flow;

import java.util.Collection;

import org.decision_deck.utils.collection.CollectionUtils;

public enum FlowType {
    POSITIVE,

    NEGATIVE,

    NET;
    static public Collection<String> strings() {
	return CollectionUtils.asStrings(values());
    }
}