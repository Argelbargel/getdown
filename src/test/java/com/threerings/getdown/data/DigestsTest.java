package com.threerings.getdown.data;

import com.threerings.getdown.DigestsTestHelper;
import com.threerings.getdown.util.DigestsUtil;
import com.threerings.getdown.util.VersionUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static com.threerings.getdown.DigestsTestHelper.createResource;
import static org.junit.Assert.*;

public class DigestsTest {

    Resource r1,r2, r3;

    @Before
    public void generateData() throws IOException {
        r1 = createResource(1024);
        r2 = createResource(1024);
        r3 = createResource(1024);
    }


    @Test
    public void testCreate() throws IOException {

        Digests d = DigestsTestHelper.createDigests(r1, r2, r3);

        assertEquals(DigestsUtil.computeResourceDigest(r1, null), d.getResourceDigest(r1));
        assertEquals(DigestsUtil.computeResourceDigest(r2, null), d.getResourceDigest(r2));
        assertEquals(DigestsUtil.computeResourceDigest(r3, null), d.getResourceDigest(r3));
        assertEquals(DigestsUtil.computeDigestsDigest(d, VersionUtil.NO_VERSION, null), d.getMetaDigest());
    }

}