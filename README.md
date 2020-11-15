![](assets/img/logo.png "Logo Title")

# lambdaroyal-dispatch
Job dispatching and executing library written in Clojure. Submit jobs that will be executed at specific point in time to named queues that have a configurable level of concurrency. lambdaroyal-dispatch handles hundreds of thousands of jobs on on bounded resources (threads, memory) for the price of CPU utilization

![](assets/img/tldr.png "TL;DR")

# Main features

* dispatch jobs to queues
* jobs are self-contained and given by
- **ts** timestamp to execute (unix epoch)
- **queue** to use
- **λ** function with no parameters
* mark failed jobs
* rerun failed jobs 

* Named queues
- number of jobs that are executed in parallel configurable per queue
- pause/resume queue


