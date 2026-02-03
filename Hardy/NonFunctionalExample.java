import io.vavr.collection.List;
import io.vavr.control.Option;


public class NonFunctionalExample {

    public static void main(String[] args) {
        List<Person> people = createPeopleList();
        List<Person> adults = filterAdults(people);
        List<Person> sortedAdults = sortByName(adults);
        List<String> names = collectNames(sortedAdults);

        Option.of(names)
                .filter(list -> !list.isEmpty())
                .peek(NonFunctionalExample::printNames)
                .onEmpty(() -> System.out.println("No names are there"));

    }

    private static List<Person> createPeopleList(){
        return List.of(
                new Person("hardy",22),
                new Person("ammar",20),
                new Person("blnd",21),
                new Person("aland",19)
        );
    }


    private static List<Person> filterAdults(List<Person> people){
        return people
                .filter(person -> person.getAge() >= 18);

    }

    private static List<Person> sortByName(List<Person> people){
        return people.sortBy(Person::getName);
    }
    private static List<String> collectNames(List<Person> people){
        return people.map(Person::getName);
    }

    private static void printNames(List<String> names) {
        names.forEach(System.out::println);
    }


}