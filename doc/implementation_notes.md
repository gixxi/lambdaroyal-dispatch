# Implementation Notes/Menthal Sidecar

## 2020-11-21

* for scheduled jobs as well as for done/failed jobs we a ref watcher
* when a job is added to a queue for scheduled jobs, the watcher checks whether there is already a thread running for this queue that handles scheduled jobs
* checking whether to exit the thread for a queue and adding the thread for this queue is synchronised 
