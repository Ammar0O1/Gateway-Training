package org.example;
import io.vavr.collection.List;
import io.vavr.control.Option;

public class Main {



    static List<Person> people = List.of(
            new Person("Alice", 30),
            new Person("Bob", 15),
            new Person("Charlie", 22),
            new Person("mike", 17));




    public static void main(String[] args) {
        List<Person> adults = filterAdults(people);
        List<Person> sortedAdultsByName = sortByName(adults);
        List<String> names = collectNames(sortedAdultsByName);


//this is another approach
        //        names.headOption()
//                .peek(ignored -> printNames(names))
//                .onEmpty(() -> System.out.println("No names found"));

        Option.of(names)
                .filter(list -> !list.isEmpty())
                .peek(x -> printNames(names))
                .onEmpty(() -> System.out.println("no names found"));






    }



    private static List<Person> filterAdults(List<Person> people) {
        return people.filter(person -> person.getAge() >= 18);

    }

    private static List<Person> sortByName(List<Person> people) {
        return people.sortBy(Person::getName);
    }

    public static List<String> collectNames(List<Person> people) {
        return people.map(Person::getName);
    }

    public static void printNames(List<String> names){
        names.forEach(System.out::println);
    }


}














