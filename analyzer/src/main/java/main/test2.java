package main;
import java.util.Scanner;
import com.github.javaparser.ast.body.Parameter;

public class test2 {
    static void sayHello(String name) {
        System.out.println("Hello, " + name + "!");
    }
    static void introduction(String name1, Scanner s1, Parameter p1) {
        sayHello(name1);
        String name5 = "name4";
        String name3 = "test";
        if(name1.equals("ok"))
        {
            name3 = "ok";
            if(name1.equals("xxx"))
            {
                name3 = name5;
            }
            name3 = "tu";

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
        Scanner scanner = new Scanner(System.in); // 用于读取系统输入
        Parameter parameter = new Parameter();
        introduction("1",scanner,parameter);
    }
}
