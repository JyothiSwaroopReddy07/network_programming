import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class filereading {


    static ExecutorService exec = Executors.newFixedThreadPool(20);

     public static void countLines(String path) {
        int lineCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            while (reader.readLine() != null) {
                lineCount++;
            }
            System.out.println("Total number of lines: " + lineCount +" in file "+ path);
        } catch (IOException e) {
            System.err.println("An error occurred while reading the file: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        
        int n=args.length;
        long totalstartTime = System.currentTimeMillis();
        for(int i=0;i<n;i++) {
            int index=i;
            Runnable task = () -> {
                long startTime = System.currentTimeMillis();
                System.out.println("Task started in: " + Thread.currentThread().getName());

                countLines(args[index]);

                long endTime = System.currentTimeMillis();
                System.out.println("Task completed in: " + Thread.currentThread().getName());
                System.out.println("Execution time: " + (endTime - startTime) + " ms");
            
            };

            exec.submit(task);
        }
        exec.shutdown();
        try {
            if (exec.awaitTermination(1, TimeUnit.HOURS)) { // Adjust time limit as needed
                long totalendTime = System.currentTimeMillis();
                System.out.println("Total program execution time: " + (totalendTime - totalstartTime) + " ms");
            } else {
                System.out.println("Timeout occurred before all tasks completed.");
            }
        } catch (InterruptedException e) {
            System.err.println("Execution was interrupted.");
            Thread.currentThread().interrupt();
        }
    }
}

// Run this command: java filereading.java "pride-and-prejudice.txt" "road_network.txt" "pride-and-prejudice.txt" "pride-and-prejudice.txt" "pride-and-prejudice.txt" "pride-and-prejudice.txt" "pride-and-prejudice.txt" "pride-and-prejudice.txt" "pride-and-prejudice.txt" "pride-and-prejudice.txt" "pride-and-prejudice.txt" "pride-and-prejudice.txt" "pride-and-prejudice.txt"

