package continuing.education.javareflection;

import lombok.val;

import java.lang.reflect.Field;
import java.util.Arrays;

public class SizeOfObject {

    private static final long HEADER_SIZE = 12;
    private static final long REFERENCE_SIZE = 4;

    public long sizeOfObject(final Object input) {
        return HEADER_SIZE + REFERENCE_SIZE + Arrays.stream(input.getClass().getDeclaredFields())
                .filter(field -> !field.isSynthetic())
                .map(field -> {
                    val type = field.getType();
                    val instance = retreive(field, input);
                    if(type.isPrimitive()) {
                        return sizeOfPrimitiveType(type);
                    } else if(type.equals(String.class)) {
                        return sizeOfString(instance.toString());
                    } else {
                        return sizeOfObject(instance);
                    }
                }).reduce(0l, Long::sum);
    }

    private Object retreive(final Field field, final Object object) {
        try{
            field.setAccessible(true);
            return field.get(object);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }


    /*************** Helper methods ********************************/
    private long sizeOfPrimitiveType(final Class<?> primitiveType) {
        if (primitiveType.equals(int.class)) {
            return 4;
        } else if (primitiveType.equals(long.class)) {
            return 8;
        } else if (primitiveType.equals(float.class)) {
            return 4;
        } else if (primitiveType.equals(double.class)) {
            return 8;
        } else if (primitiveType.equals(byte.class)) {
            return 1;
        } else if (primitiveType.equals(short.class)) {
            return 2;
        }
        throw new IllegalArgumentException(String.format("Type: %s is not supported", primitiveType));
    }

    private long sizeOfString(final String inputString) {
        int stringBytesSize = inputString.getBytes().length;
        return HEADER_SIZE + REFERENCE_SIZE + stringBytesSize;
    }
}
