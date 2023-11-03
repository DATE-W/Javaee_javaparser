package animal;

class Main {
    public void speak() {
        Animal a = new Dog();
        a.speak(); // It actually invokes the speak method of the Dog class
    }
}