package continuing.education.javareflection;

import lombok.AllArgsConstructor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Class.getDeclaredFields() returns all fields including statics, no matter the modifier. Excludes inherited fields.
 *
 * Class.getFields() returns all public fields, including inherited fields.
 *
 * Class.getDeclaredField(fieldName) gets the field by its name
 *
 * Synthetic fields are fields generated by the java compiler for its internal usage. We would stumble upon these at runtime
 * if we are using reflection. These are specific to the compiler, and rarely of use to the developer.
 * Can be checked with the Field.isSynthetic() method.
 */
public class AboutFields {

    public static void main(String[] args) throws IllegalAccessException, NoSuchFieldException {
        printDeclaredFields(Category.class, Category.ACTION);

        /*
         * An instance of the class is necessary to get the actual value of a given field when calling the
         * Field.get(instance) method.
         */
        Movie movie = new Movie(15);
        printDeclaredFields(Movie.class, movie);

        /*
         * The only exception to the rule above are static fields, which are stored in the class
         * definition itself. If you are trying to get the value of a static field, you can pass `null` to this
         * method, and it will still work.
         */
        Field staticMovieField = Movie.class.getDeclaredField("MIN_PRICE");

        System.out.println(staticMovieField.get(null));
    }

    public static <T> void printDeclaredFields(Class<? extends T> clz, T instance) throws IllegalAccessException {
        System.out.println(clz.getName());
        for(Field field : clz.getDeclaredFields()) {
            System.out.println(field.getName());
            int mod = field.getModifiers();
            System.out.println("abstract " + Modifier.isAbstract(mod));
            System.out.println("final " + Modifier.isFinal(mod));
            System.out.println("native " + Modifier.isNative(mod));
            System.out.println("private " + Modifier.isPrivate(mod));
            System.out.println("protected " + Modifier.isProtected(mod));
            System.out.println("public " + Modifier.isPublic(mod));
            System.out.println("static " + Modifier.isStatic(mod));
            System.out.println("volatile " + Modifier.isVolatile(mod));
            System.out.println("strict " + Modifier.isStrict(mod));
            System.out.println("sync " + Modifier.isSynchronized(mod));
            System.out.println("trans " + Modifier.isTransient(mod));
            System.out.println(field.getType().getName());
            System.out.println(field.isSynthetic());
            System.out.println(field.get(instance));
        }
    }

    @AllArgsConstructor
    public static class Movie {
        private static final double MIN_PRICE = 10.99;

        double price;

        /*
         MovieStats will have another field inside it that serves as the reference to Movie, so that
         the MovieStats object at runtime can get access to the Movie.price field. It will look something like
         Field name: this$0, type: my.package.AboutFields$Movie
         */
        @AllArgsConstructor
        public class MovieStats {
            double timesWatched;

            public double getRevenue() {
                return timesWatched * price;
            }

        }
    }

    public enum Category {
        ROMCOM,
        ACTION,
        SUSPENSE;
    }
}

