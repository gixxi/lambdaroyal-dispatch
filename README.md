![](assets/img/logo.png "Logo Title")

# lambdaroyal-dispatch
Job dispatching and executing library written in Clojure. Submit jobs that will be executed at specific point in time to named queues that have a configurable level of concurrency. lambdaroyal-dispatch handles hundreds of thousands of jobs on on bounded resources (threads, memory) for the price of CPU utilization

# dependency

```clojure
:dependencies [[org.clojars.gixxi/lambdaroyal-dispatch "0.8"]]
```

# TL;DR

![](assets/img/tldr.png "TL;DR")

# Main features

* dispatch jobs to queues
* jobs are self-contained and given by
  - **ts** timestamp to execute (unix epoch)
  - **queue** to use
  - **λ** function with no parameters
* mark failed jobs
* rerun failed jobs 

# Usage

Creating a dispatcher

```clojure
(require '[lambdaroyal.dispatch.core :refer :all])

(def d (dispatcher))

```

Creating a dispatcher with custom parameters

```clojure
(require '[lambdaroyal.dispatch.core :refer :all])

(def d (dispatcher
         :verbose true
         :frequency 100
         :on-failed (fn [job] (println (format "job [%s] failed. execution (ms) [%s] error message [%s]" (:id job) (- (:stopped-at job) (:started-at job)) (:error-message job))))
         :on-done (fn [job] (println (format "jobs [%s] done")))
         :on-start (fn [job] (println (format "jobs [%s] started")))))

```

Dispatch a job

```Clojure
(dispatch d
          "Datasink" ;; Queuename
          (+ (System/currentTimeMillis) 1000) ;; execute in one minute
          ;; every second job fails
          (fn [] 
            (swap! res conj x)))
```

# Testing

> lein midje

# Constraints

## Queue Name

The name of a queue is an arbitrary scalar, e.g. one of the following

* String literals "Foo"
* Numbers like 1 or 5.4
* Keywords like :Bar

## Execution order of jobs

Assume two Jobs A,B on Queue Q. It is ensured that Job A starts and finishes before B whene the execution time of A is below the execution time of B.

If Two jobs A,B on Queue have the same execution time, then the execution order of both is non-deterministic.

## Side-effects

The integrity of the queues is guaranteed using software transactional memory. Pending Jobs are executed in bulk after the queues where modified by the dispatcher. 

Side-effects are allowed in job lambdas and lifecycle methods like on *on-stop*.

# Rationale 

Planing and executing jobs considering a certain schedule is a common task of information systems. Lambdaroyal-dispatch helps to group jobs as per the execution unit (the queue) in order to allow to pose certain concurrency constraints on jobs. Furthermore lambdaroyal-dispatch keeps track of the job state and manages resource utilization to allow for handling *hundreds of thousands* of jobs on bounded resources.

## The alternatives 

One could use Clojure's concurrency abstractions - delays, futures, promises - or even the Java abstractions. BUT one has to to take care about resource utilization and pose certain constraints on the jobs to be dispatched to achieve correct behaviour.

### Job execution order

If job A gets dispatched at dispatch time (DT) DT1 for execution time (ET) ET1 and job B gets dispatched at DT2 for execution time ET2 and the following holds true

> DT1 < DT2 < now && ET1 > ET2

then there is no guarantee using a fixed-bound thread pool that job B gets executed before job A since there is the possibility that the number of jobs that gets dispatched after job A and before job B exceeds the number of remaining threads in the bounded-size thread pool. **ENSURES CORRECT EXECUTION ORDER**

### Concurrent jobs in one queue

One could use *core/async* channels with limited buffer size or thread-pools with the size as per the desired level of parallelism. Or one could use a semaphore which is exactly what lambdaroyal-dispatch does. **HIDES BOILERPLATE**