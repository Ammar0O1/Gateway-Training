package com.manager;
import io.vavr.collection.List;
import io.vavr.control.Option;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {

        List<Person> people = createPeopleList();
//        people.forEach(System.out::println);

        List<Person> adults = filterAdults(people);
//        adults.forEach(System.out::println);
        List<Person> sorted = sortByNameAndAge(adults);


//        sorted.forEach(System.out::println);



        Option<List<String>> namesOpt = Option.when(!sorted.isEmpty(),collectNames(sorted));
        List<String> names = namesOpt.getOrElse(List.empty()); //getOrElse() preffered over fold() not suitable for our code it over complicates the code for no reason

        names.forEach(System.out::println);
    }
    private static List<Person> createPeopleList(){
        return List.of(new Person("John", 30),
                        new Person("Jane", 25),
                        new Person("Alice",23),
                        new Person ("hardy",16),
                        new Person("Ammar", 40),
                        new Person("Alisson", 22),
                        new Person ("Mami gawra",64)
                        );
    }
    private static List<Person> filterAdults(List<Person> people){//to filter adults here by using filter
        return people.filter(person -> person.getAge() >= 18);
    }
    private static List<Person> sortByNameAndAge(List<Person> people){      //we sorted by names alphabetically and age by using the sortBy method which is a simple call
    return people.sortBy(p->io.vavr.Tuple.of(p.getName(),p.getAge()));      //tuple is used to apply more than one method of sorting if alphabetically equal use age
    }
    private static List<String>collectNames(List<Person> people){
        return people.map(Person::getName);
    }
}
