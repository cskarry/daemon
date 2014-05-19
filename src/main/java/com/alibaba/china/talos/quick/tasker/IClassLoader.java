package com.alibaba.china.talos.quick.tasker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IClassLoader extends ClassLoader {

    private static Map<String, File> pathMap = new ConcurrentHashMap<String, File>();

    public IClassLoader(ClassLoader parent){
        super(parent);
    }

    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes = loadClassBytes(name);
        Class<?> theClass = defineClass(name, bytes, 0, bytes.length);
        if (theClass == null) {
            throw new ClassFormatError();
        }
        return theClass;
    }

    private byte[] loadClassBytes(String className) throws ClassNotFoundException {
        try {
            String classFile = getClassFile(className);
            FileInputStream fis = new FileInputStream(classFile);
            FileChannel fileC = fis.getChannel();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            WritableByteChannel outC = Channels.newChannel(baos);
            ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
            while (true) {
                int i = fileC.read(buffer);
                if (i == 0 || i == -1) {
                    break;
                }
                buffer.flip();
                outC.write(buffer);
                buffer.clear();
            }
            fis.close();
            return baos.toByteArray();
        } catch (IOException fnfe) {
            throw new ClassNotFoundException(className);
        }
    }

    private String getClassFile(String name) {
        String classFileName = classToPath(name);
        Collection<File> fileList = pathMap.values();
        for (File file : fileList) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (null != files && files.length > 0) {
                    for (File metaFile : files) {
                        if (metaFile.isDirectory()) {
                            continue;
                        }
                        if (metaFile.getName().equals(classFileName)) {
                            return metaFile.getAbsolutePath();
                        }
                    }
                }
            }
        }
        return "";
    }

    private String classToPath(String className) {
        int dot_index = className.lastIndexOf(".");
        return className.substring(dot_index + 1) + ".class";
    }

    public static void refreshContext(String path) {
        if (!pathMap.containsKey(path)) {
            File f = new File(path);
            if (f.isDirectory()) {
                pathMap.put(path, f);
            }
        }
    }

    public static void main(String[] args) {
        IClassLoader ins = new IClassLoader(null);
        ins.getClassFile("com.alibaba.karry");
    }

}
