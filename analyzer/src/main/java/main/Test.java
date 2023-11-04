package main;
class Test {
    static void sayHello(String name) {
        System.out.println("Hello, " + name + "!");
    }
    static void introduction(String name1) {
        sayHello(name1);
        String name2 = "Garfield";
        sayHello(name2);
    }

    static void introduction(String name1,String StuNumber) {
        sayHello(name1);
        String name2 = "Garfield";
        String StuNumber2=StuNumber;
        sayHello(name2);
    }
    public static void main(String[] args) {
        sayHello("Jon");
        String name3 = "Odie";
        introduction(name3);
        test1 t1 = new test1();
        t1.sayHello("123");
    }
}