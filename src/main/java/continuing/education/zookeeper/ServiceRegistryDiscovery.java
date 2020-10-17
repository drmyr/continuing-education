package continuing.education.zookeeper;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.apache.zookeeper.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * In ZK, we are going to create a ZNode that stores the address of each instance in the cluster. Then, on startup,
 * each new service is going to register itself under that znode with its specific address. This way, at any time, a
 * given instance can call out to zookeeper to get the
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceRegistryDiscovery implements Watcher {

    private static final String SERVICE_REGISTRY = "/service_registry";

    final ZooKeeper zooKeeper;
    String serviceRegistryPath;
    List<String> allServiceAddresses;

    public ServiceRegistryDiscovery(final ZooKeeper zooKeeper) throws InterruptedException {
        this.zooKeeper = zooKeeper;
        createServiceRegistryZNode();
    }

    public void registerUpdates() {
        try {
            updateAddresses();
        } catch (final KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public synchronized List<String> getAllServiceAddresses() throws KeeperException, InterruptedException {
        if(isNull(allServiceAddresses)) {
            updateAddresses();
        }
        return this.allServiceAddresses;
    }

    public void unregisterFromCluster() {
        try {
            if(nonNull(this.serviceRegistryPath) && nonNull(zooKeeper.exists(serviceRegistryPath, false))) {
                zooKeeper.delete(serviceRegistryPath, -1);
            }
        } catch (final InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }

    private void createServiceRegistryZNode() throws InterruptedException {
        try {
            if(isNull(zooKeeper.exists(SERVICE_REGISTRY, false))) {
                zooKeeper.create(SERVICE_REGISTRY, new byte[] {}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (final KeeperException e) {
            // KeeperException here would be the result of a race condition, in which `exists` returns false for
            // two callers, and so both callers attempt to `create` the registry. ZK server will make sure that only one
            // znode gets created, but the second caller would get an exception.
            e.printStackTrace();
        }
    }

    public void registerToCluster(final String metaData) throws KeeperException, InterruptedException {
        this.serviceRegistryPath = zooKeeper.create(SERVICE_REGISTRY + "/n_", metaData.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
    }

    private synchronized void updateAddresses() throws KeeperException, InterruptedException {
        val workerZNodes = zooKeeper.getChildren(SERVICE_REGISTRY, this);
        val addresses = new ArrayList<String>(workerZNodes.size());
        for(val workZNode : workerZNodes) {
            val fullPath = SERVICE_REGISTRY + "/" + workZNode;
            // potential for race condition here, where a node that we were told about in the `getChildren` call
            // has gone down
            val stat = zooKeeper.exists(fullPath, false);
            if(isNull(stat)) continue;

            addresses.add(new String(zooKeeper.getData(fullPath, false, stat)));
        }
        this.allServiceAddresses = Collections.unmodifiableList(addresses);
    }

    @Override
    public void process(final WatchedEvent event) {
        try {
            updateAddresses();
        } catch (final KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
