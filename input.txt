import B;
class A {
    public int foo(B b, boolean a) {
        return 0;
    }
    public B bar() {
        return new B();
    }
    public int testing() {
        boolean p2;
        p2 = this.bar().another();
        this.foo(this.bar(), p2);
        return 0;
    }
}