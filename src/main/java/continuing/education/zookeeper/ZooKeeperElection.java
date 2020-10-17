package continuing.education.zookeeper;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.isNull;

@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ZooKeeperElection implements Watcher {

    private static final String ELECTION_NAMESPACE = "/election";

    OnElectionCallback callback;
    ZooKeeper zk;
    AtomicReference<String> myZNode = new AtomicReference<>();

    public String volunteerForLeadership() throws KeeperException, InterruptedException {
        val znodePrefix = ELECTION_NAMESPACE + "/c_";
        // 1. if we disconnect from ZK, the znode will be deleted
        // 2. the order sequence number will be appended to the znode name, depending on order of creation.
        val myZNode = zk.create(znodePrefix, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        this.myZNode.set(myZNode);
        return myZNode;
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
    public void electLeader() throws KeeperException, InterruptedException {
        Stat predecessorStat;
        do {
            val children = zk.getChildren(ELECTION_NAMESPACE, false);
            Collections.sort(children);
            val amLeader = myZNode.get().endsWith(children.get(0));
            if(amLeader) {
                System.out.println("I am the leader");
                callback.onElectedToBeLeader();
                return;
            } else {
                // if not elected leader, then set up subscription to the (myZNode - 1) zNode, so that if (zNode - 1) fails,
                // we are notified, and can update our succession chain.
                System.out.println("I am not the leader");
                val predecessorIndex = Collections.binarySearch(children, myZNode.get().replace(ELECTION_NAMESPACE + "/", "")) - 1;
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

        callback.onWorker();
    }

    @Override
    public void process(final WatchedEvent event) {
        switch (event.getType()) {
            case NodeDeleted:
                try {
                    electLeader();
                } catch (final KeeperException | InterruptedException e) {
                    e.printStackTrace();
                }
        }
    }
}
