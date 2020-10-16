package continuing.education.zookeeper;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.Collections;
import java.util.function.Function;

import static java.util.Objects.isNull;

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

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ZooKeeperThreadingModelExample implements Watcher {
    private static final String ZK_ADDY = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000;
    private static final String ELECTION_NAMESPACE = "/election";

    Object monitor;
    ZooKeeperThreadingModelExample() {
        this.monitor = new Object();
    }

    public static void main(final String[] args) throws InterruptedException, KeeperException {
        final Function<Watcher, ZooKeeper> connectToZK = watcher -> {
            try {
                return new ZooKeeper(ZK_ADDY, SESSION_TIMEOUT, watcher);
            } catch (final IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        };
        val election = new ZooKeeperThreadingModelExample();
        val zk = connectToZK.apply(election);
        val myZNode = election.volunteerForLeadership(zk);
        election.electLeader(zk, myZNode);
        election.watchTargetZNode(zk);
        election.run();
        election.close(zk);
    }

    public void run() throws InterruptedException {
        synchronized (monitor) {
            monitor.wait();
        }
    }

    public void close(final ZooKeeper zk) throws InterruptedException {
        zk.close();
    }

    public String volunteerForLeadership(final ZooKeeper zk) throws KeeperException, InterruptedException {
        val znodePrefix = ELECTION_NAMESPACE + "/c_";
        // 1. if we disconnect from ZK, the znode will be deleted
        // 2. the order sequence number will be appended to the znode name, depending on order of creation.
        return zk.create(znodePrefix, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
    }

    /*
     When new leader election needs to happen, we want to avoid the "Herd Effect" wherein all subscribed nodes attempt
     to become the leader at once, when they are all notified that the leader node has failed. If many nodes are subscribed,
     and they all start to react when the leader goes down, this will spark a large amount of CPU & network activity
     within the cluster at once, leading to bottlenecks and potential failures.

     Instead, there should be a succession chain, wherein leadership is passed down according to znode election order,
     IE, the first node to register is the leader, the second node to register will be the leader if the first node fails,
     and so on.
     */
    public void electLeader(final ZooKeeper zk, final String myZNode) throws KeeperException, InterruptedException {
        Stat predecessorStat = null;
        do {
            val children = zk.getChildren(ELECTION_NAMESPACE, false);
            Collections.sort(children);
            val amLeader = myZNode.endsWith(children.get(0));
            if(amLeader) {
                System.out.println("I am the leader");
            } else {
                // if not elected leader, then set up subscription to the (myZNode - 1) zNode, so that if (zNode - 1) fails,
                // we are notified, and can update our succession chain.
                System.out.println("I am not the leader");
                val predecessorIndex = Collections.binarySearch(children, myZNode) - 1;
                val predecessorZNodeName = children.get(predecessorIndex);
                // There is a race condition here, because we are operating on the `children` that were returned from the
                // `getChildren` call, but what if one of those children went down after the call completed?
                // then, instead of getting back the intended predecessor znode, we would be getting back `null` here.

                // as such, we need to run in a while loop until we get back a non null predecessor
                // THIS IS WHERE THE PREDECESSOR IN THE CHAIN IS SELECTED.
                // the selection mechanism goes back to the fact that when the `exists` method is called, our
                // Watcher is registered as a one time event handler for any events associated to the ZNode
                // it is registered to.

                // Remember, the method will only run if the znode we are interested in has an event.
                predecessorStat = zk.exists(ELECTION_NAMESPACE + "/" + predecessorZNodeName, this);
            }
        } while(isNull(predecessorStat));

    }

    /**
     * When we register a {@code Watcher} with {@code ZooKeeper::exists}, {@code ZooKeeper::getChildren}, or {@code ZooKeeper::getData},
     * we get a one-time trigger that will be invoked on that {@code Watcher}
     */
    public void watchTargetZNode(final ZooKeeper zk) throws KeeperException, InterruptedException {
        val targetZNode = "/target_znode";
        val stat = zk.exists(targetZNode, this);
        if(stat == null) return; // is znode does not exist, then null is returned

        val data = zk.getData(targetZNode, this, stat);
        val children = zk.getChildren(targetZNode, this);

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
                    synchronized (monitor) {
                        System.out.println("Disconnect from ZK server");
                        monitor.notifyAll();
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

        /*
         If you want persistent event subscription, you will have to re-run `watchTargetZNode(ZooKeeper)` here
         to create another subscription
         */
    }
}
