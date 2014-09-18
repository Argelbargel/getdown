package com.threerings.getdown;

import com.threerings.getdown.data.Digests;
import com.threerings.getdown.data.Resource;
import com.threerings.getdown.data.ResourceGroup;
import com.threerings.getdown.data.ResourceType;
import com.threerings.getdown.util.ProgressObserver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.threerings.getdown.util.VersionUtil.NO_VERSION;

public class DigestsTestHelper {
    public static byte[] generateData(long length) {
        String charset = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZüöä?!%&+~\n\t\r";
        Random rnd = new Random();

        StringBuilder sb = new StringBuilder();
        for( int i = 0; i < length; i++ ) {
            sb.append(charset.charAt(rnd.nextInt(charset.length())));
        }

        return sb.toString().getBytes();
    }

    public static Resource createResource(long length) throws IOException {
        return FileTestHelper.createLocalResource(ResourceType.RESOURCE_FILE, createTempFile(generateData(length)));
    }

    public static File createTempFile(byte[] data) throws IOException {
        File tmpFile = FileTestHelper.createTempFile();

        FileOutputStream out = new FileOutputStream(tmpFile);
        out.write(data);
        out.close();
        return tmpFile;
    }

    public static Digests createDigests(byte[]... data) throws IOException {
        ResourceGroup rg = new ResourceGroup();

        for (byte[] d : data) {
            rg.addResources(FileTestHelper.createLocalResource(ResourceType.RESOURCE_FILE, createTempFile(d)));
        }

        return Digests.create(rg, NO_VERSION);
    }

    public static Digests createDigests(Resource... resources) throws IOException {
        ResourceGroup rg = new ResourceGroup();
        rg.addResources(resources);

        return Digests.create(rg, NO_VERSION);
    }


    public static class ProgressObserverStub implements ProgressObserver {
        private final List<Integer> calls = new ArrayList<Integer>();
        @Override
        public void progress(int percent) {
            calls.add(percent);
        }

        public List<Integer> getCalls() {
            return calls;
        }
    }
}
