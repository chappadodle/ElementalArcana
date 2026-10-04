package com.chappadodle.elementalarcana.content.creature;

import com.chappadodle.elementalarcana.api.Element;

/** A creature that is a drop of one element (a wisp, or a familiar): drawn by client/WispRenderer. */
public interface ElementalOrb {
    Element element();
}
