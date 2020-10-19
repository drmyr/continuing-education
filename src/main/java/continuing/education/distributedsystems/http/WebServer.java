package continuing.education.distributedsystems.http;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class WebServer {

    private static final String TASK = "/task";
    private static final String STATUS = "/status";
}
