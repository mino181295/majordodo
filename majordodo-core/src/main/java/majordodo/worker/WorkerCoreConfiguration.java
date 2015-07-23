/*
 Licensed to Diennea S.r.l. under one
 or more contributor license agreements. See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership. Diennea S.r.l. licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.

 */
package majordodo.worker;

import majordodo.task.Task;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import majordodo.utils.ReflectionUtils;

/**
 * configuration of the worker
 *
 * @author enrico.olivelli
 */
public class WorkerCoreConfiguration {

    private int maxThreads;
    private String workerId;
    private String location;
    private Map<String, Integer> maxThreadsByTaskType;
    private List<Integer> groups;
    private int tasksRequestTimeout = 60000;

    public WorkerCoreConfiguration() {
        maxThreadsByTaskType = new HashMap<>();
        maxThreadsByTaskType.put(Task.TASKTYPE_ANY, 1);
        maxThreads = 20;
        location = "unknown";
        groups = new ArrayList<>();
        groups.add(0);
    }

    public void read(Map<String, Object> properties) {
        ReflectionUtils.apply(properties, this);
    }

    /**
     * Maximum number of threads used to process tasks
     *
     * @return
     */
    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    /**
     * Worker id (not the processId!)
     *
     * @return
     */
    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    /**
     * Description of the location, usually is something like
     * InetAddress.getLocalhost()...
     *
     * @return
     */
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Maximum number of active threads per tasktype
     *
     * @return
     */
    public Map<String, Integer> getMaxThreadsByTaskType() {
        return maxThreadsByTaskType;
    }

    public void setMaxThreadsByTaskType(Map<String, Integer> maxThreadsByTaskType) {
        this.maxThreadsByTaskType = maxThreadsByTaskType;
    }

    /**
     * User groups, in order of priority
     *
     * @return
     */
    public List<Integer> getGroups() {
        return groups;
    }

    public void setGroups(List<Integer> groups) {
        this.groups = groups;
    }

    /**
     * Maximum timeout while waiting for new tasks from the broker
     *
     * @return
     */
    public int getTasksRequestTimeout() {
        return tasksRequestTimeout;
    }

    public void setTasksRequestTimeout(int tasksRequestTimeout) {
        this.tasksRequestTimeout = tasksRequestTimeout;
    }

}