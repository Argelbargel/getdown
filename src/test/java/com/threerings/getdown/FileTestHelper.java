package com.threerings.getdown;

import com.threerings.getdown.data.Resource;
import com.threerings.getdown.data.ResourceType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;

public class FileTestHelper {
    public static Resource createLocalResource(ResourceType type, File file) throws MalformedURLException {
        File appdir = file.getParentFile();
        return Resource.create(type, appdir, appdir.toURI().toURL(), file.getName());
    }

    public static File getTempDirectory() throws IOException {
        return createTempFile().getParentFile();
    }

    public static File createTempFile() throws IOException {
        return createTempFile(".data");
    }

    public static File createTempFile(byte[] data) throws IOException {
        File tmpFile = createTempFile();

        FileOutputStream out = new FileOutputStream(tmpFile);
        out.write(data);
        out.close();
        return tmpFile;
    }

    public static File createTempFile(String suffix) throws IOException {
        File tmpFile = File.createTempFile(FileTestHelper.class.getName(), suffix);
        tmpFile.deleteOnExit();
        return tmpFile;
    }
}
