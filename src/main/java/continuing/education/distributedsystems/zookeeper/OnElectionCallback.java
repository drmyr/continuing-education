package continuing.education.distributedsystems.zookeeper;

/**
 * Used to keep the service discovery and registry logic separate from the election logic
 */
public interface OnElectionCallback {

    void onElectedToBeLeader();

    void onWorker();
}
