package continuing.education.javareflection;

import lombok.*;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Scanner;

public class ConfigFileParserForArrays {

    public static void main(final String[] args) throws InvocationTargetException, IOException, InstantiationException, NoSuchMethodException, IllegalAccessException {
        val gameConfig = buildConfig(GameConfig.class, Path.of("build/resources/main/game-properties.cfg"));
        val uiConfig = buildConfig(UserInterface.class, Path.of("build/resources/main/user-interface.cfg"));

        System.out.println(gameConfig.toString());
        System.out.println(uiConfig.toString());

        val res1 = concat(1, 2, 3, new int[] {4, 5, 6}, 7);

        System.out.println(res1);

        val res2 = concat( new String[]{"a", "b"}, "c", new String[] {"d", "e"});

        System.out.println(res2);
    }

    /*
      Important to note that if the object has a final field that is not assigned in the constructor, then setting that
      field reflectively might be problematic. For example, if the field is final and assigned at compile time, the compiler
      can inline the value in the toString() method. If the value was reflectively re-assigned at runtime, the toString()
      method would have no knowledge of this:

      class Thing {
        final int value = 10; <-- compile time constant

        String toString() {
            return value; <-- will return 10, even if you reflectively re-assign the `value` field.
        }
      }
     */
    public static <T> T buildConfig(final Class<T> type, final Path configFilePath)
            throws IllegalAccessException, InvocationTargetException, InstantiationException, IOException, NoSuchMethodException {
        val scanner = new Scanner(configFilePath); // to read the config file

        val ctor = type.getDeclaredConstructor();
        ctor.setAccessible(true);

        val newInstance = ctor.newInstance();

        while(scanner.hasNextLine()) {
            val kvp = scanner.nextLine().split("=");

            try {
                val field = type.getDeclaredField(kvp[0]);
                field.setAccessible(true);

                val fieldType = field.getType();
                if(fieldType.isArray()) {
                    field.set(newInstance, parseArray(fieldType.getComponentType(), kvp[1]));
                } else {
                    field.set(newInstance, parseValue(fieldType, kvp[1]));
                }
            } catch (final NoSuchFieldException ex) { // if the field is undefined, swallow exception and continue
                System.err.println("property undefined: " + kvp[0]);
            }
        }

        return newInstance;
    }

    public static Object parseArray(final Class<?> arrayComponentType, final String entry) {
        val elements = entry.split(",");
        val newArr = Array.newInstance(arrayComponentType, elements.length);
        for(int i = 0; i < elements.length; i++) {
            Array.set(newArr, i, parseValue(arrayComponentType, elements[i]));
        }

        return newArr;
    }

    private static Object parseValue(final Class<?> type, final String fieldValue) {
        if(type.equals(int.class)) return Integer.parseInt(fieldValue);
        if(type.equals(short.class)) return Short.parseShort(fieldValue);
        if(type.equals(long.class)) return Long.parseLong(fieldValue);
        if(type.equals(double.class)) return Double.parseDouble(fieldValue);
        if(type.equals(float.class)) return Float.parseFloat(fieldValue);
        return fieldValue;
    }

    public static Object concat(final Object... arguments) {

        if (arguments.length == 0) {
            return new Object[]{};
        }

        final ArrayDeque<Object> queue = new ArrayDeque<>();
        for (val argument : arguments) {
            final Class<?> type = argument.getClass();
            if (type.isArray()) {
                for (int j = 0; j < Array.getLength(argument); j++) {
                    queue.addLast(Array.get(argument, j));
                }
            } else {
                queue.addLast(argument);
            }
        }

        return queue.toArray();
    }


    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class GameConfig {
        int year;
        String name;
        double price;
        String[] characterNames;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class UserInterface {
        String titleColor;
        String titleText;
        int[] titleFontSizes;
        String[] titleFonts;
        int footerFontSize;
    }
}
