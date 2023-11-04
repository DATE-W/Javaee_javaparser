package main;

public class test2 {
    static void sayHello(String name) {
        System.out.println("Hello, " + name + "!");
    }
    static void introduction(String name1) {
        sayHello(name1);
        String name3 = "test";
        if(name1.equals("ok"))
        {
            name3 = "ok";
        }
        else
        {
            name3 = "not";
        }
        String name2 = name3;
        sayHello(name2);
        Test t = new Test();
        t.sayHello("111111");
        t.sayHello("222222");
    }

    static void main(String args[])
    {
        introduction("1");
    }
}
