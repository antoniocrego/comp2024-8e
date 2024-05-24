## Group members

- António Rego (up202108666@edu.fe.up.pt)
- Diogo Fernandes (up202108752@edu.fe.up.pt)
- João Pereira (up202108848@edu.fe.up.pt)

## Work Distribution

- António Rego - 33.33 %
- Diogo Fernandes - 33.33 %
- João Pereira - 33.33 %

## Self-Assessment

- We think our project deserves a 16 out of 20.

## Extra Elements

- Our project supports Constant Propagation and Constant Folding with the "-o" flag set to either "true", for optimizing, or "false", this being the default. This only propagates literal constants, as it could, in theory, propagate code like "1+this.foo()" as long as this variable was never changed, however, this would add more operations done for anytime the variable would be referenced.

- With the "-r=<n>" the registers used in Ollir and Jasmin can be optimized, with -1 for no optimization, 0 for optimization on, and >1 for choosing a maximum number of registers used, being that the compilation fails and stops halfway in the case of not having enough registers.
