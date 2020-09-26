package continuing.education.javareflection;

import lombok.val;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

import static continuing.education.models.Models.*;

public class DIYJSONSerializer {

    public static void main(String[] args) throws IllegalAccessException {
        val addy = new Address("Avenue Street", (short) 1);

        val person = new Person("Bob", true, 29, 100.20f, addy);

        System.out.println(toJson(person));

        val actor = new Actor("A person", new String[] {"first movie", "second movie"});

        System.out.println(toJson(actor));

        val movie = new Movie("LOR", 8.8f, new String[]{"Action", "Adventure", "Drama"});

        System.out.println(toJson(movie));
    }

    public static String toJson(final Object obj) throws IllegalAccessException {
        val sb = new StringBuilder("{");

        val fields = obj.getClass().getDeclaredFields();

        for(int i = 0; i < fields.length; i++) {
            if(fields[i].isSynthetic()) continue;

            fields[i].setAccessible(true);

            sb.append(wrapWithQuotes(fields[i].getName())).append(":");

            val type = fields[i].getType();
            if(fields[i].get(obj) == null) {
                sb.append("null");
            } else if(type.isPrimitive()) {
                sb.append(primitiveToString(fields[i].get(obj)));
            } else if(type.equals(String.class)) {
                sb.append(wrapWithQuotes(fields[i].get(obj).toString()));
            } else if(type.isArray()) {
                sb.append(arrayToJson(fields[i].get(obj)));
            } else {
                sb.append(toJson(fields[i].get(obj)));
            }

            if(i != fields.length - 1) {
                sb.append(",");
            }
        }

        return sb.append("}").toString();
    }

    private static String arrayToJson(final Object instance) throws IllegalAccessException {
        val sb = new StringBuilder("[");

        val componentType = instance.getClass().getComponentType();

        val arrLen = Array.getLength(instance);

        for(int i = 0; i < arrLen; i++) {
            val elem = Array.get(instance, i);

            if(componentType.isPrimitive()) {
                sb.append(primitiveToString(elem));
            } else if(componentType.equals(String.class)) {
                sb.append(wrapWithQuotes(elem.toString()));
            } else { // array contains another object
                sb.append(toJson(elem));
            }

            if(i != arrLen - 1) {
                sb.append(",");
            }
        }

        return sb.append("]").toString();
    }

    private static String primitiveToString(final Object instance) {
        val type = instance.getClass();
        if(type.equals(double.class) || type.equals(float.class)) return String.format("%.02f", instance);
        return instance.toString();
    }

    private static String primitiveToString(Field field, Object instance) throws IllegalAccessException {
        val type = field.getType();
        val obj = field.get(instance);
        if(type.equals(double.class) || type.equals(float.class)) return String.format("%.02f", obj);
        return obj.toString();
    }

    private static String wrapWithQuotes(String str) {
        return String.format("\"%s\"", str);
    }
}
