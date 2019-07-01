package com.applitools.eyes.selenium.positioning;

import com.applitools.eyes.Location;
import com.applitools.eyes.positioning.PositionMemento;

import java.util.Map;

/**
 * Encapsulates state for {@link CssTranslatePositionProvider} instances.
 */
public class CssTranslatePositionMemento extends PositionMemento {
    private final String transform;
    private final Location position;

    /**
     * Instantiates a new Css translate position memento.
     *
     * @param transform The current transform.
     * @param position  the position
     */
    public CssTranslatePositionMemento(String transform, Location position) {
        this.transform = transform;
        this.position = position;
    }

    /**
     *
     * @return The current transforms. The keys are the style keys from
     * which each of the transforms were taken.
     * @return The current transforms. The keys are the style keys from which each of the transforms were taken.
     */
    public String getTransform() {
        return transform;
    }

    /**
     * Gets position.
     *
     * @return the position
     */
    public Location getPosition() { return position; }

    @Override
    public int getX() {
        return position.getX();
    }

    @Override
    public int getY() {
        return position.getY();
    }
}
