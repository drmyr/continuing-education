package continuing.education.models;

import lombok.Value;

public enum Models {
    ;
    @Value public static class Address { String street; short number; }
    @Value public static class Person { String name; boolean employed; int age; float salary; Address address;}
    @Value public static class Actor { String name; String[] movies; }
    @Value public static class Movie {String name; float rating; String[] categories; }
}
