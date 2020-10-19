package continuing.education.distributedsystems.http;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class RequestAggregator {

    WebClient webClient;

    public RequestAggregator() {
        this.webClient = new WebClient();
    }

    public List<String> sendTasksToWorkers(final List<String> workerAddresses, final List<String> tasks) {
        final CompletableFuture<String>[] futures = new CompletableFuture[workerAddresses.size()];

        for(int i = 0; i < workerAddresses.size(); i++) {
            val addy = workerAddresses.get(i);
            val task = tasks.get(i);
            futures[i] = webClient.sendTask(addy, task.getBytes());
        }

        return Stream.of(futures).map(CompletableFuture::join).collect(Collectors.toList());
    }
}
