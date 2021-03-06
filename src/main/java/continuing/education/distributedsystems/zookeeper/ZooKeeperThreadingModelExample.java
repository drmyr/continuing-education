package continuing.education.distributedsystems.zookeeper;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.apache.zookeeper.*;

import java.io.IOException;

/**
 * use the zkCli.sh script to communicate with the zookeeper server over command line.
 *
 * For this example, inside the zookeeper instance just run
 * `create /election ""`
 * to create the necessary znode
 *
 * The zookeeper library creates two threads: Event Thread & IO Thread
 * IO Thread: not much direct developer manipulation, responsible for creating and maintaining connection to ZK server.
 *      Handles: pings, session management, session timeouts, requests, responses
 * Event Thread: manages events such as connecting, disconnecting, custom znode watchers and triggers
 *
 * ZK uses log4j
 *
 *
 */

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ZooKeeperThreadingModelExample implements Watcher {
    private static final String ZK_ADDY = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000;

    @Setter
    ZooKeeper zooKeeper;

    public static void main(final String[] args) throws InterruptedException, KeeperException, IOException {

        val threadingModel = new ZooKeeperThreadingModelExample();
        val zk = new ZooKeeper(ZK_ADDY, SESSION_TIMEOUT, threadingModel);
        threadingModel.setZooKeeper(zk);

        val service = new ServiceRegistryDiscovery(zk);
        val onElection = new OnElectionActon(service, 8080);
        val elector = new ZooKeeperElection(onElection, zk);

        val myZNode = elector.volunteerForLeadership();
        elector.electLeader();
        threadingModel.watchTargetZNode();
        threadingModel.run();
        threadingModel.close();
    }

    public void run() throws InterruptedException {
        synchronized (zooKeeper) {
            zooKeeper.wait();
        }
    }

    public void close() throws InterruptedException {
        zooKeeper.close();
    }

    /**
     * When we register a {@code Watcher} with {@code ZooKeeper::exists}, {@code ZooKeeper::getChildren}, or {@code ZooKeeper::getData},
     * we get a one-time trigger that will be invoked on that {@code Watcher}
     */
    public void watchTargetZNode() throws KeeperException, InterruptedException {
        val targetZNode = "/target_znode";
        val stat = zooKeeper.exists(targetZNode, this);
        if(stat == null) return; // is znode does not exist, then null is returned

        val data = zooKeeper.getData(targetZNode, this, stat);
        val children = zooKeeper.getChildren(targetZNode, this);

        System.out.println("data: " + new String(data) + " children: " + children);

        /*
         in a terminal running zkCli.sh

         create /target_znode "test data"

        remember that this method will only run once. You have to re-register every time you want this method to run.

         */
    }

    /**
     * Will be called from the ZK library on a separate thread whenever a new event comes in from the server.
     * @param event
     */
    @Override
    public void process(final WatchedEvent event) {
        switch (event.getType()) {
            case None: //zk connection events don't have a type
                if(event.getState() == Event.KeeperState.SyncConnected) {
                    // this state means successful connection to zk server
                    System.out.println("connected to ZK server");
                } else { // if zookeeper sends a disconnect event, it will be handled here.
                    synchronized (zooKeeper) {
                        System.out.println("Disconnect from ZK server");
                        zooKeeper.notifyAll();
                    }
                }
                break;
            case NodeDeleted:
                System.out.println("NodeDeleted " + event.getPath());
                // to loop the leader re-election algorithm, a call to
                // electLeader(ZooKeeper,String) would have to be performed here.
                break;
            case NodeCreated:
                System.out.println("NodeCreated " + event.getPath());
                break;
            case NodeDataChanged:
                System.out.println("NodeDataChanged " + event.getPath());
                break;
            case NodeChildrenChanged:
                System.out.println("NodeChildrenChanged " + event.getPath());
                break;
        }

        try {
            watchTargetZNode();
        } catch (final KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
