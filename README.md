# Concurrent Session Store Simulation

## Overview

This is a Concurrent Session Store Simulation for Assignment 2 of KT44502 Distributed Systems Sem 1 Session 2026/27. The application is implemented in Clojure and demonstrates concurrent state management using functional programming principles.

In modern distributed web applications, concurrent requests modify user session states simultaneously. This application simulates a concurrent session store where multiple background worker threads create, update, and expire user sessions concurrently, while a background reader thread safely observes point-in-time consistent snapshots without blocking writers.

This is a **single-machine, multi-process simulation**. All processes run on the same Linux VM, it is not an actual multi-machine network cluster.

## Requirements

- Linux VM (tested on Ubuntu 22.04.5)
- Java JDK 11 or higher
- Clojure CLI tools

## Run on Linux

Open a terminal in this folder.

```bash
chmod +x run.sh
sh "./run.sh"
```

or alternatively the Clojure script can be run diretly.

```bash
clojure project.clj
```

## Functional programming concepts demonstrated

1. **Pure functions**: State transition functions (`create-session`, `record-activity-safe`, 
   `expire-session`) always return the exact same output map with zero side effects or
   external mutations, given the same input maps and arguments.
2. **Immutable data structures**: All session maps and records utilize Clojure’s persistent, 
   immutable data structures. Updates are performed via structural sharing (`assoc`, `update`,
   `assoc-in`) rather than mutating memory in place.
3. **Higher-order functions**: The codebase leverages functions that accept or return other 
   functions, such as `mapv`, `filter`, `reduce`, and passing pure update functions directly into concurrency primitives like `swap!`.
4. **Stateless computation**: Business logic calculations do not maintain local mutable 
   variables or internal states. Computations derive their results purely from explicit 
   parameters.
5. **Functional concurrency models**: Concurrency is managed through Clojure atoms and 
   optimistic locking (`swap!`). Instead of OS locks or semaphores, threads coordinate
   updates using Compare-And-Swap (CAS) mechanics backed by immutable state.
6. **Referential transparency**: Because state-transition functions are pure, any function
   call can be safely substituted with its evaluated return value without altering the behavior or correctness of the program.
