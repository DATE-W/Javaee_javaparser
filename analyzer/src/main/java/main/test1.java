package main;

public class test1 {
    static void sayHello(String name) {
        System.out.println("Hello, " + name + "!");
    }
    static void introduction(String name1) {
        sayHello(name1);
        String name2 = "Garfield";
        name2 = "fdsfds";
        sayHello(name2);
        Test t = new Test();
        t.sayHello("hello");
    }
}
