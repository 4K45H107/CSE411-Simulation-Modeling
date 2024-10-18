import math
import random

# Constants
BUSY = 1
IDLE = 0
Q_LIMIT = 100


# Simulation class
class simulation:
    def __init__(self):
        
        #  output files
        self.file = open('output.txt', 'w')
        self.file2 = open('output2.txt', 'w')

        # printing variables
        self.eventNo = 1
        self.arrivalNo = 1
        self.departureNo = 1
        
        self.numEvents = 2
        self.timeArrival = [0.0] * (Q_LIMIT + 1)  # time of arrival of the i-th customer
        

    def initialize(self):
        
        # initialize simulation clock
        self.simulationTime = 0.0

        # initialize state variables
        self.serverStatus = IDLE
        self.numberInQueue = 0
        self.timeLastEvent = 0.0

        # initialize statistical counters
        self.numberOfCustomerDelayed = 0
        self.totalDelay = 0.0
        self.areaNumberInQueue = 0.0
        self.areaServerStatus = 0.0

        # initialize event list
        self.timeNextEvent = [0.0] * (self.numEvents + 1)

        # First event
        self.timeNextEvent[1] = self.simulationTime + self.exponential(self.meanInterarrival)
        self.timeNextEvent[2] = 1.0e+30

    def exponential(self, mean):
        
        # generate exponential variate with mean "mean"
        return -mean * math.log(random.random())

    def timing(self):
        
        minTimeNextEvent = 1.0e+29
        self.nextEventType = 0

        # Event type of next event
        for i in range(1, self.numEvents + 1):
            if self.timeNextEvent[i] < minTimeNextEvent:
                minTimeNextEvent = self.timeNextEvent[i]
                self.nextEventType = i

        # Check to see whether the event list is empty, if it is stop the simulation
        if self.nextEventType == 0:
            self.file.write(f"Event list is empty at time {self.simulationTime}\n\n")
            return
        
        # the event list is not empty, so advance the simulation clock
        self.simulationTime = minTimeNextEvent
    
    
    def arrive(self):

        self.file2.write(f"{self.eventNo}. Next Event: Customer {self.arrivalNo} Arrival\n")
        self.eventNo += 1
        self.arrivalNo += 1
        self.file2.write(f"\n----------No. of customer delayed {self.numberOfCustomerDelayed+1}----------\n\n")

        
        # schedule next arraval
        self.timeNextEvent[1] = self.simulationTime + self.exponential(self.meanInterarrival)
        
        # check if server is busy
        if self.serverStatus == BUSY:
            
            # server is busy, increment number in queue
            self.numberInQueue += 1
            
            # check if queue is full
            if self.numberInQueue > Q_LIMIT:
                self.file.write(f"Overflow of the array timeArrival at {self.simulationTime}\n\n")
                return
            
            # still room in queue, store the arrived customer
            self.timeArrival[self.numberInQueue] = self.simulationTime
        
        else:
            
            # server is idle, so arriving customer has a delay of zero
            delay = 0.0
            self.totalDelay += delay
            
            # increment number of customers delayed, and make server busy
            self.numberOfCustomerDelayed += 1
            self.serverStatus = BUSY
            
            # schedule a departure (service completion)
            self.timeNextEvent[2] = self.simulationTime + self.exponential(self.meanService)

    def depart(self):
        
        self.file2.write(f"{self.eventNo}. Next Event: Customer {self.departureNo} Deputure\n")
        self.eventNo += 1
        self.departureNo += 1

        # check if queue is empty
        if self.numberInQueue == 0:
            
            # queue is empty, so make server idle
            self.serverStatus = IDLE
            self.timeNextEvent[2] = 1.0e+30
        
        else:
            
            # queue is not empty, decrement number in queue
            self.numberInQueue -= 1
            
            # compute delay of customer who is beginning service and update total delay
            delay = self.simulationTime - self.timeArrival[1]
            self.totalDelay += delay
            
            # increment number of customers delayed, and schedule departure
            self.numberOfCustomerDelayed += 1
            self.timeNextEvent[2] = self.simulationTime + self.exponential(self.meanService)
            
            # move each customer in queue (if any) up one place
            for i in range(1, self.numberInQueue + 1):
                self.timeArrival[i] = self.timeArrival[i + 1]


    def report(self):
        
        # self.file.write results of the simulation
        self.file.write(f"Average delay in queue {self.totalDelay / self.numberOfCustomerDelayed:.3f}\n\n" )
        self.file.write(f"Average number in queue {self.areaNumberInQueue / self.simulationTime:.3f}\n\n")
        self.file.write(f"Server utilization {self.areaServerStatus / self.simulationTime:.3f}\n\n")
        self.file.write(f"Time simulation ended {self.simulationTime:.3f}\n\n")

    def updateTimeAverageStats(self):
        
        # compute the time since last event
        timeSinceLastEvent = self.simulationTime - self.timeLastEvent
        self.timeLastEvent = self.simulationTime
        
        # update area under number-in-queue function
        self.areaNumberInQueue += self.numberInQueue * timeSinceLastEvent
        
        # update area under server-busy indicator function
        self.areaServerStatus += self.serverStatus * timeSinceLastEvent

        

    def run(self):
        
        # Read input Parameters
        with open('input.txt', 'r') as file:
            line = file.readline()
            meanInterarrival, meanService, numDelaysRequired = [float(val) for val in line.split()]

            # input from fileS
            self.meanInterarrival = meanInterarrival
            self.meanService = meanService
            self.numDelaysRequired = numDelaysRequired 

        # write report heading
        self.file.write(f"Single-server queueing system\n")
        self.file.write(f"------------------------------\n\n")
        self.file.write(f"Mean interarrival time   -> {self.meanInterarrival} minutes\n\n")
        self.file.write(f"Mean service time        -> {self.meanService} minutes\n\n")
        self.file.write(f"Number of customers      -> {self.numDelaysRequired}\n\n\n\n")
        
        # initialize the simulation
        self.initialize()
        
        # run the simulation while more delays are still needed
        while self.numberOfCustomerDelayed < self.numDelaysRequired:

            # determine the next event
            self.timing()
            
            # update time-average statistical accumulators
            self.updateTimeAverageStats()
            
            # invoke the appropriate event function
            if self.nextEventType == 1:
                self.arrive()
            elif self.nextEventType == 2:
                self.depart()
            
        # invoke the report generator
        self.report()
    
    


offline1 = simulation()
simulation.run(offline1)


