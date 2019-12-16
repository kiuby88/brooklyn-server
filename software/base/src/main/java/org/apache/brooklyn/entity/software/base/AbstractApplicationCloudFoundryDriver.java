/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.brooklyn.entity.software.base;

import org.apache.brooklyn.api.entity.EntityLocal;
import org.apache.brooklyn.location.cloudfoundry.CloudFoundryPaasLocation;
import org.apache.brooklyn.util.core.task.DynamicTasks;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.Beta;

@Beta
public abstract class AbstractApplicationCloudFoundryDriver
        extends AbstractSoftwareProcessCloudFoundryDriver
        implements ApplicationCloudFoundryDriver {

    public static final Logger log = LoggerFactory.getLogger(AbstractApplicationCloudFoundryDriver.class);


    public AbstractApplicationCloudFoundryDriver(EntityLocal entity,
                                                 CloudFoundryPaasLocation location) {
        super(entity, location);
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public SoftwareProcessImpl getEntity() {
        return (SoftwareProcessImpl) super.getEntity();
    }

    protected abstract String getApplicationUrl();

    public abstract String getApplicationName();

    public abstract String getBuildpack();

    @Override
    public boolean isRunning() {
        CloudApplication app = getClient().getApplication(getApplicationName());
        return (app != null)
                && app.getState().equals(CloudApplication.AppState.STARTED);
    }

    @Override
    public int getInstancesNumber() {
        CloudApplication app = getClient().getApplication(getApplicationName());
        return app.getInstances();
    }

    @Override
    public int getDisk() {
        CloudApplication app = getClient().getApplication(getApplicationName());
        return app.getDiskQuota();
    }

    @Override
    public int getMemory() {
        CloudApplication app = getClient().getApplication(getApplicationName());
        return app.getMemory();
    }

    @Override
    public void start() {


        super.start();
        DynamicTasks.queue("pre deploy", new Runnable() {
            public void run() {
                preDeploy();
            }
        });
        DynamicTasks.queue("deploy", new Runnable() {
            public void run() {
                deploy();
            }
        });
        DynamicTasks.queue("pre-launch", new Runnable() {
            public void run() {
                preLaunch();
            }
        });
        DynamicTasks.queue("launch", new Runnable() {
            public void run() {
                launch();
            }
        });
        DynamicTasks.queue("post-launch", new Runnable() {
            public void run() {
                postLaunch();
            }
        });
    }

    public void preDeploy() {
    }

    public abstract void deploy();

    public void preLaunch() {
    }

    public void launch() {
        getClient().startApplication(getApplicationName());
    }

    public void postLaunch() {
    }

    @Override
    public void restart() {
        if (getClient().getApplication(getApplicationName()).getState() == CloudApplication.AppState.STOPPED) {
            getClient().startApplication(getApplicationName());
        } else {
            getClient().restartApplication(getApplicationName());
        }
    }

    @Override
    public void stop() {
        super.stop();
        try{
            getClient().stopApplication(getApplicationName());
        //deleteApplication();
        } catch (Exception e){
            log.error("***** Error calling to cloudFoundry STOP effector : " + e.getMessage());
        }
    }

    @Override
    public void deleteApplication() {
        try{
            log.info("************************ DELETING from driver-->" + getApplicationName());
            getClient().deleteApplication(getApplicationName());
            log.info("************************ DELETED from driver-->" + getApplicationName());
        } catch (Exception e){
            log.error("***** Error calling to cloudFoundry DELETE operation: " + e.getMessage());
        }
    }

    protected String inferApplicationDomainUri(String name) {
        String defaultDomainName = getClient().getDefaultDomain().getName();
        return name + "-domain." + defaultDomainName;
    }


}
