package com.threerings.getdown.data;

import com.threerings.getdown.FileTestHelper;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;

import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.hasItems;

public class ResourceGroupTest {
    Resource r1 = null;
    Resource r2 = null;
    Resource r3 = null;

    @Before
    public void createResources() throws MalformedURLException {
        r1 = FileTestHelper.createLocalResource(ResourceType.RESOURCE_CODE, new File("./test1"));
        r2 = FileTestHelper.createLocalResource(ResourceType.RESOURCE_FILE, new File("./test2"));
        r3 = FileTestHelper.createLocalResource(ResourceType.RESOURCE_ARCHIVE, new File("./test3"));
    }

    @Test
    public void testResourceGroup() throws Exception {
        ResourceGroup rg = new ResourceGroup();
        assertEquals("", rg.getName());
        assertEquals(0, rg.size());
        assertTrue(rg.isEmpty());
        assertFalse(rg.hasSubgroups());
    }

    @Test
    public void testAddResources() throws Exception {
        ResourceGroup rg = new ResourceGroup();

        rg.addResources(r1, r2, r3);

        assertEquals(3, rg.size());
        assertThat(rg.getResources(), hasItems(r1, r2, r3));
    }

    @Test(expected = NullPointerException.class)
    public void testAddResourcesNull() throws Exception {
        new ResourceGroup().addResources((Resource[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddResourcesContainingNull() throws Exception {
        new ResourceGroup().addResources(r1, null, r2);
    }



    @Test
    public void testGetResources() throws Exception {
        ResourceGroup rg = new ResourceGroup();

        rg.addResources(r1, r2);

        assertThat(rg.getResources(), hasItems(r1, r2));
        assertThat(rg.getResources(ResourceType.ANY), hasItems(r1, r2));
        assertThat(rg.getResources(ResourceType.RESOURCE_CODE), hasItems(r1));
        assertThat(rg.getResources(ResourceType.RESOURCE_FILE), hasItems(r2));
        assertTrue(rg.getResources(ResourceType.CONFIG_FILE).isEmpty());
    }

    @Test
    public void testGetSubgroup() throws Exception {
        ResourceGroup rg = new ResourceGroup();
        ResourceGroup sg = rg.getSubgroup("sub");

        assertEquals(sg, rg.getSubgroup("sub"));
        assertNotSame(sg, rg.getSubgroup("sub"));

        assertEquals("sub", sg.getName());
        assertEquals(0, sg.size());
        assertTrue(sg.isEmpty());
        assertFalse(sg.hasSubgroups());
        assertTrue(sg.getSubgroups().isEmpty());
    }

    @Test
    public void testAddResourcesToSubGroup() throws Exception {
        ResourceGroup rg = new ResourceGroup();
        ResourceGroup sg = rg.getSubgroup("sub");

        sg.addResources(r1, r2);

        assertEquals(2, sg.size());
        assertThat(sg.getResources(), hasItems(r1, r2));

        assertTrue(rg.hasSubgroups());
        assertThat(rg.getSubgroups(), hasItems(sg));
        assertEquals(0, rg.size());
        assertTrue(rg.isEmpty());
    }

    @Test
    public void testClear() throws Exception {
        ResourceGroup rg = new ResourceGroup();
        ResourceGroup sg1 = rg.getSubgroup("sub1");
        ResourceGroup sg2 = rg.getSubgroup("sub2");


        rg.addResources(r1);
        sg1.addResources(r2);
        sg2.addResources(r3);

        sg1.clear();

        assertEquals(0, sg1.size());
        assertEquals(1, sg2.size());
        assertEquals(1, rg.size());

        rg.clear();

        assertEquals(1, sg2.size());
        assertEquals(0, rg.size());
    }
}