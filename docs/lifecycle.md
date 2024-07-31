# Component lifecycle

New one:

- `[<fn> <opts> <children>]`
- make-impl: call <fn> -> record instance
- set element
- dirty := true

During draw/measure:

- maybe-render
  - if dirty?
    - reconcile-impl
    - update-element
    - dirty := false

Reused:

- should-reconcile?
- reconcile
  - reconcile-impl
  - update-element
  - set element
