package continuing.education.javareflection;

import com.sun.net.httpserver.HttpServer;
import lombok.Getter;

import static continuing.education.models.Models.*;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;

/**
 * Class.getDeclaredConstructors() -> all constructors of the class, does not include inherited constructors
 * Class.getConstructors() -> only public constructors, but includes inherited ones as well
 *
 * If you know the Constructor you are looking for (based on the types it takes) you can call
 * Class.getConstructor(Class<?>... types) or Class.getDeclaredConstructor(Class<?>... types) to get the specific one.
 * If no Constructor matches, you will get a NoSuchMethodException
 *
 * If you call Class.getConstructor() or Class.getDeclaredConstructor() without arguments, you will get the no-args ctor,
 * if such a ctor exists.
 *
 * Constructor.newInstance(Object... args) calls the ctor of the class that has matching arguments, in the provided order
 * It gives access to all ctor's, regardless of their access modifiers, unless the class is in a Java module that
 * does not allow access.
 */
public class AboutConstructors {

    public static void main(String[] args) throws Exception {
        printCtorData(Person.class);

        Person person = createClassWithArgs(Person.class);
        System.out.println(person);

        initServer();
        new WebServer().startServer();
    }

    public static <T> T createClassWithArgs(Class<T> clz, Object... args) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        for (Constructor<?> ctor : clz.getDeclaredConstructors()) {
            if (ctor.getParameterTypes().length == args.length) {
                return (T) ctor.newInstance(args);
            }
        }
        throw new UnsupportedOperationException("cant create instance of " + clz.getSimpleName());
    }

    public static void printCtorData(Class<?> clazz) {
        final Constructor<?>[] ctors = clazz.getDeclaredConstructors();

        System.out.println(String.format("class %s has %d declared ctors", clazz.getSimpleName(), ctors.length));

        for (final Constructor<?> ctor : ctors) {
            final Class<?>[] paramTypes = ctor.getParameterTypes();
            for (final Class<?> name : paramTypes) {
                System.out.println(name.getSimpleName());
            }
            System.out.println("#############################");
        }
    }

    // EXAMPLE WITH WEB SERVER

    public static void initServer() throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {
        final Constructor<ServerConfig> ctor = ServerConfig.class.getDeclaredConstructor(int.class, String.class);
        ctor.setAccessible(true);
        System.out.println(ctor.newInstance(8080, "Good day"));
    }

    @Getter
    public static class ServerConfig {
        private static ServerConfig serverConfigInstance;

        final InetSocketAddress inetSocketAddress;
        final String greetingMessage;

        private ServerConfig(int port, String greetingMessage) {
            this.greetingMessage = greetingMessage;
            this.inetSocketAddress = new InetSocketAddress("localhost", port);

            if(serverConfigInstance == null) {
                serverConfigInstance = this;
            }
        }

        public static ServerConfig getInstance() {
            return serverConfigInstance;
        }
    }

    public static class WebServer {

        public void startServer() throws IOException {
            final InetSocketAddress addy = ServerConfig.getInstance().getInetSocketAddress();
            final HttpServer httpServer = HttpServer.create(addy, 0);
            httpServer.createContext("/greeting").setHandler(exchange -> {
                final String respMsg = ServerConfig.getInstance().greetingMessage;
                exchange.sendResponseHeaders(200, respMsg.length());

                final OutputStream respBody = exchange.getResponseBody();
                respBody.write(respMsg.getBytes());
                respBody.flush();
                respBody.close();
            });

            System.out.println(String.format("starting server on addy %s:%d", addy.getHostName(), addy.getPort()));

            httpServer.start();
        }
    }
}
