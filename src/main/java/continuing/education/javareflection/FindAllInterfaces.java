package continuing.education.javareflection;

import lombok.val;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public class FindAllInterfaces {

    public Set<Class<?>> findAllImplementedInterfaces(final Class<?> input) {
        class Recurse {
            private Set<Class<?>> findRec(final List<Class<?>> inputInterfaces, final HashSet<Class<?>> accum) {
                if(inputInterfaces.isEmpty()) {
                    return accum;
                } else {
                    accum.addAll(inputInterfaces);
                    val next = inputInterfaces.stream()
                            .flatMap(interf -> Arrays.stream(interf.getInterfaces()))
                            .collect(toList());
                    return findRec(next, accum);
                }
            }
        }

        return new Recurse().findRec(asList(input.getInterfaces()), new HashSet<>());
    }
}
