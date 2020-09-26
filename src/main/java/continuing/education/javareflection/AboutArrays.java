package continuing.education.javareflection;

import lombok.val;

import java.lang.reflect.Array;

public class AboutArrays {

    public static void main(final String[] args) {
        final int[] oneDarr = {1, 2};

        final double[][] twoDarr = {{1.1, 2.2}, {3.3, 4.4}};

        inspectArrays(oneDarr);

        inspectArrays(twoDarr);

        inspectArrayValues(twoDarr);
    }

    public static void inspectArrayValues(final Object array) {
        val arrLength = Array.getLength(array);

        System.out.print("[");
        for(int i = 0; i < arrLength; i++) {
            val obj = Array.get(array, i);

            if(obj.getClass().isArray()) {
                inspectArrayValues(obj);
            } else {
                System.out.print(obj);
            }

            if(i != arrLength - 1) {
                System.out.print(",");
            }
        }
        System.out.print("]");
    }

    public static void inspectArrays(final Object obj) {
        val clz = obj.getClass();

        System.out.println(String.format("is array %s", clz.isArray()));

        val componentType = clz.getComponentType();

        System.out.println(String.format("component type %s", componentType.getTypeName()));
    }
}
