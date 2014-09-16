package com.threerings.getdown.data;

import java.util.Arrays;
import java.util.List;

/**
 * The major steps involved in updating, along with some arbitrary percentages
 * assigned to them, to mark global progress.
 */
public enum Step
{
    UPDATE_JAVA(10),
    VERIFY_METADATA(15, 65, 95),
    DOWNLOAD(40),
    PATCH(60),
    VERIFY_RESOURCES(70, 97),
    REDOWNLOAD_RESOURCES(90),
    UNPACK(98),
    LAUNCH(99);

    /** What is the final percent value for this step? */
    public final List<Integer> defaultPercents;

    /** Enum constructor. */
    Step(Integer... percents)
    {
        this.defaultPercents = Arrays.asList(percents);
    }
}
