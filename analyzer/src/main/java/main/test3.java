package main;

class Animal {
    void speak() { System.out.println("Animal speaks"); }
}
class Dog extends Animal {
    @Override
    void speak() { System.out.println("Dog barks"); }
}
class test3 {
    class test6 {
        public void test5() {
            class test4 {
                public void speak() {
                    int A = 0;
                    int k = 1;
                    Animal b = new Dog();
                    switch (A) {
                        case 0:
                            Animal a=new Animal();
                            if (k > 0) {
                                b.speak(); // It actually invokes the speak method of the Dog class
                            } else {
                                a.speak(); // It actually invokes the speak method of the Dog class
                            }
                        case 1:
                            b.speak();
                    }
                }
            }
        }
    }
}