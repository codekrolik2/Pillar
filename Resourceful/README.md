TODO: Advanced features
Idea: Lock queue + deadlock resolution

1) Support for immediate lock on multiple resources (lock all or nothing)
2) Request tries to lock immediately, if that fails - adds a request record to the lock table
3) 2-phase Lock request: submit, evaluate
4) Priority for lock requests - no locks allowed for lower priority requests if higher priority request is waiting for the same resource (directly or indirectly)
5) Deadlock detection on request evaluation - traverse lockers dependency graph as follows:
	1. Find all resources that need to be locked, extract blocking locker list out of those of them that are locked (ignore original locker for 1st level)
	2. Find all lock requests for the blocking lockers
	3. Do the same as in 1 for the lock requests to find next level of lockers
	4. If at any time during this traversal an original locker is encountered - a deadlock is found.

2-phase Lock requests and deadlock detection will require a dedicated leader server to load data periodically and process requests.
Which might be just a WerkFlow! 

NOT DO:
Semaphore locks - no clear use case for semaphore locks, and provided new deadlock resolution functionality it adds a lot of complexity to implementation.