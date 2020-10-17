package continuing.education.zookeeper;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.apache.zookeeper.KeeperException;

import java.net.InetAddress;
import java.net.UnknownHostException;

@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class OnElectionActon implements OnElectionCallback {

    ServiceRegistryDiscovery registryDiscovery;
    int port;

    @Override
    public void onElectedToBeLeader() {
        // if promoted to leader, unregister from registry
        registryDiscovery.unregisterFromCluster();
        registryDiscovery.registerUpdates();
    }

    @Override
    public void onWorker() {
        try {
            val currentServiceAddy = String.format("http://%s:%d", InetAddress.getLocalHost().getCanonicalHostName(), port);
            registryDiscovery.registerToCluster(currentServiceAddy);
        } catch (final UnknownHostException | InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }
}
