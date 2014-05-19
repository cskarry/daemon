package com.alibaba.china.talos.service.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Set;

import com.alibaba.citrus.service.resource.Resource;
import com.alibaba.citrus.service.resource.ResourceLoadingOption;
import com.alibaba.citrus.service.resource.ResourceLoadingService;
import com.alibaba.citrus.service.resource.ResourceNotFoundException;
import com.alibaba.citrus.service.resource.ResourceTrace;


public class MockedResourceLoadingService implements ResourceLoadingService {

    @Override
    public ResourceLoadingService getParent() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public URL getResourceAsURL(String resourceName) throws ResourceNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public File getResourceAsFile(String resourceName) throws ResourceNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public File getResourceAsFile(String resourceName, Set<ResourceLoadingOption> options)
                                                                                          throws ResourceNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String resourceName) throws ResourceNotFoundException, IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Resource getResource(String resourceName) throws ResourceNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Resource getResource(String resourceName, Set<ResourceLoadingOption> options)
                                                                                        throws ResourceNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean exists(String resourceName) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ResourceTrace trace(String resourceName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResourceTrace trace(String resourceName, Set<ResourceLoadingOption> options) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] list(String resourceName) throws ResourceNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] list(String resourceName, Set<ResourceLoadingOption> options) throws ResourceNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Resource[] listResources(String resourceName) throws ResourceNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Resource[] listResources(String resourceName, Set<ResourceLoadingOption> options)
                                                                                            throws ResourceNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getPatterns(boolean includeParent) {
        // TODO Auto-generated method stub
        return null;
    }

}
