package com.applitools.eyes.positioning;

import com.applitools.eyes.RegionF;

/**
 * Encapsulates a getRegion "callback" and how the region's coordinates should be used.
 */
public interface RegionProvider {
    /**
     * @return A region with "as is" viewport coordinates.
     */
    RegionF getRegion();
}
