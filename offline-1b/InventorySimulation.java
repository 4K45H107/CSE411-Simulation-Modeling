import java.io.*;
import java.util.*;
import java.util.stream.IntStream;

public class InventorySimulation {
    private static final double INF = 1e30;
    private static final int ORDER_ARRIVAL = 1;
    private static final int DEMAND = 2;
    private static final int EVALUATE = 4;
    private static final int REPORT = 3;

    private int totalUnits;
    private int largeLimit;
    private int initialInventoryLevel;
    private int currentInventoryLevel;
    private int numOfDemandValues;
    private List<Double> probDemandDistribution;
    private int currentDemandAmount;
    private int minimumInventoryLimit;
    private double maximumInventoryLimit;
    private double minimumDeliveryLag;
    private double maximumDeliveryLag;
    private double areaUnderInventory;
    private double areaUnderStockout;
    private double holdingCost;
    private double incrementalOrderCostPerUnit;
    private double restockingCost;
    private double backlogCost;
    private double totalOrderingCost;
    private double setupCost;
    private double averageDemandInterval;
    private double simulationTime;
    private double timeLastEvent;
    private double[] nextEventTime;
    private int nextEventType;
    private int numOfMonths;
    private int numPolicies;
    private List<Policy> policies;

    public InventorySimulation() {
        this.probDemandDistribution = new ArrayList<>();
        this.policies = new ArrayList<>();
        this.nextEventTime = new double[5];
    }

    private static class Policy {
        int smallLimit;
        int largeLimit;

        Policy(int smallLimit, int largeLimit) {
            this.smallLimit = smallLimit;
            this.largeLimit = largeLimit;
        }
    }

    private void initialize() {
        currentInventoryLevel = initialInventoryLevel;
        timeLastEvent = 0.0;
        simulationTime = 0.0;
        totalOrderingCost = 0.0;
        areaUnderInventory = 0.0;
        areaUnderStockout = 0.0;
        nextEventTime[ORDER_ARRIVAL] = INF;
        nextEventTime[DEMAND] = simulationTime + exponential(averageDemandInterval);
        nextEventTime[REPORT] = numOfMonths;
        nextEventTime[EVALUATE] = 0.0;
        nextEventTime[0] = 1e31;
    }

    private void timing() {
        double minimumTime = Arrays.stream(nextEventTime).min().orElse(INF);
        simulationTime = minimumTime;
        nextEventType = IntStream.range(0, nextEventTime.length).filter(i -> nextEventTime[i] == minimumTime).findFirst().orElse(-1);
    }

    private void orderArrival() {
        currentInventoryLevel += currentDemandAmount;
        nextEventTime[ORDER_ARRIVAL] = INF;
    }

    private void demand() {
        int demand = randomInteger(probDemandDistribution);
        currentInventoryLevel -= demand;
        double nextDemand = exponential(averageDemandInterval);
        nextEventTime[DEMAND] = simulationTime + nextDemand;
    }

    private void evaluate() {
        if (currentInventoryLevel < minimumInventoryLimit) {
            currentDemandAmount = (int) (maximumInventoryLimit - currentInventoryLevel);
            double extra = setupCost + incrementalOrderCostPerUnit * currentDemandAmount;
            totalOrderingCost += extra;
            nextEventTime[ORDER_ARRIVAL] = simulationTime + uniform();
        }
        nextEventTime[EVALUATE] = simulationTime + 1.0;
    }

    private void report(BufferedWriter writer) throws IOException {
        double avgOrderingCost = totalOrderingCost / numOfMonths;
        double avgHoldingCost = holdingCost * areaUnderInventory / numOfMonths;
        double avgShortageCost = backlogCost * areaUnderStockout / numOfMonths;

        writer.write(String.format("(%2d, %3d)%1s%17.2f%23.2f%24.2f%20.2f%n",
                minimumInventoryLimit, (int) maximumInventoryLimit, " ",
                avgOrderingCost + avgHoldingCost + avgShortageCost, avgOrderingCost,
                avgHoldingCost, avgShortageCost));
    }

    private void updateTimeAvgStats() {
        double timeSinceLastEvent = simulationTime - timeLastEvent;
        timeLastEvent = simulationTime;
        if (currentInventoryLevel < 0) {
            areaUnderStockout -= currentInventoryLevel * timeSinceLastEvent;
        } else {
            areaUnderInventory += currentInventoryLevel * timeSinceLastEvent;
        }
    }

    private double exponential(double mean) {
        return -mean * Math.log(Math.random());
    }

    private int randomInteger(List<Double> probDistrib) {
        double u = Math.random();
        for (int i = 0; i < probDistrib.size(); i++) {
            if (u < probDistrib.get(i)) {
                return i;
            }
        }
        return probDistrib.size() - 1;
    }

    private double uniform() {
        return minimumDeliveryLag + (maximumDeliveryLag - minimumDeliveryLag) * Math.random();
    }

    private void readAndWriteFiles(String inputFilename, String outputFilename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilename));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilename))) {
            String[] firstLine = reader.readLine().split(" ");
            initialInventoryLevel = Integer.parseInt(firstLine[0]);
            numOfMonths = Integer.parseInt(firstLine[1]);
            numPolicies = Integer.parseInt(firstLine[2]);

            String[] secondLine = reader.readLine().split(" ");
            double demandValues = Double.parseDouble(secondLine[0]);
            averageDemandInterval = Double.parseDouble(secondLine[1]);

            String[] thirdLine = reader.readLine().split(" ");
            setupCost = Double.parseDouble(thirdLine[0]);
            incrementalOrderCostPerUnit = Double.parseDouble(thirdLine[1]);
            holdingCost = Double.parseDouble(thirdLine[2]);
            backlogCost = Double.parseDouble(thirdLine[3]);

            String[] fourthLine = reader.readLine().split(" ");
            minimumDeliveryLag = Double.parseDouble(fourthLine[0]);
            maximumDeliveryLag = Double.parseDouble(fourthLine[1]);

            String[] probDemandValues = reader.readLine().split(" ");
            for (String value : probDemandValues) {
                probDemandDistribution.add(Double.parseDouble(value));
            }

            for (int i = 0; i < numPolicies; i++) {
                String[] policyValues = reader.readLine().split(" ");
                int smallLimit = Integer.parseInt(policyValues[0]);
                int largeLimit = Integer.parseInt(policyValues[1]);
                policies.add(new Policy(smallLimit, largeLimit));
            }

            writer.write("------Single-Product Inventory System------\n\n");
            writer.write("Initial inventory level: " + initialInventoryLevel + " items\n\n");
            writer.write("Number of demand sizes: " + (int) demandValues + "\n\n");
            writer.write("Distribution function of demand sizes: ");
            for (Double prob : probDemandDistribution) {
                writer.write(String.format("%.2f ", prob));
            }
            writer.write("\n\n");
            writer.write("Mean inter-demand time: " + String.format("%.2f", averageDemandInterval) + " months\n\n");
            writer.write("Delivery lag range: " + String.format("%.2f to %.2f months\n\n", minimumDeliveryLag, maximumDeliveryLag));
            writer.write("Length of simulation: " + numOfMonths + " months\n\n");
            writer.write("Costs:\n");
            writer.write("K = " + String.format("%.2f\n", setupCost));
            writer.write("i = " + String.format("%.2f\n", incrementalOrderCostPerUnit));
            writer.write("h = " + String.format("%.2f\n", holdingCost));
            writer.write("pi = " + String.format("%.2f\n\n", backlogCost));
            writer.write("Number of policies: " + numPolicies + "\n\n");
            writer.write("Policies:\n");
            writer.write("--------------------------------------------------------------------------------------------------\n");
            writer.write(" Policy        Avg_total_cost     Avg_ordering_cost      Avg_holding_cost     Avg_shortage_cost\n");
            writer.write("--------------------------------------------------------------------------------------------------\n");

            for (Policy policy : policies) {
                minimumInventoryLimit = policy.smallLimit;
                maximumInventoryLimit = policy.largeLimit;
                initialize();

                while (true) {
                    timing();
                    updateTimeAvgStats();

                    if (nextEventType == ORDER_ARRIVAL) {
                        orderArrival();
                    } else if (nextEventType == DEMAND) {
                        demand();
                    } else if (nextEventType == EVALUATE) {
                        evaluate();
                    } else if (nextEventType == REPORT) {
                        report(writer);
                        break;
                    }
                }
            }

            writer.write("--------------------------------------------------------------------------------------------------\n");
        }
    }

    public static void main(String[] args) {
        InventorySimulation sim = new InventorySimulation();
        try {
            sim.readAndWriteFiles("in.txt", "output.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
