package continuing.education.javareflection;

import lombok.val;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class RecursivelyBuildDependencies {

    /*
     recursively walks the dependency graph of construction until a no-arg dependency is found.
     */
    public static <T> T createObjsRec(final Class<T> clz) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        val ctor = getFirstCtor(clz);

        val instances = new ArrayList<Object>();

        for(val argType : ctor.getParameterTypes()) {
            val arg = createObjsRec(argType);
            instances.add(arg);
        }

        ctor.setAccessible(true);
        return (T) ctor.newInstance(instances.toArray());
    }

    private static Constructor<?> getFirstCtor(Class<?> clz) {
        val ctors = clz.getDeclaredConstructors();
        return ctors[0];
    }
}
