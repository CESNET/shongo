package cz.cesnet.shongo.measurement.launcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfilerResult
{
    private Map<String, Process> map = new HashMap<String, Process>();

    public void add(String output)
    {
        String[] lines = output.split("\n");
        for ( String line : lines ) {
            String[] values = line.trim().split("\\s+");
            Process process = new Process(values);
            if ( process.getPid().equals("PID") )
                continue;
            if ( map.containsKey(process.getPid()) ) {
                map.get(process.getPid()).merge(process);
            }
            else {
                map.put(process.getPid(), process);
            }
        }
    }
    
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("    PID | Max Threads | Avg CPU | Max Memory | Command\n");
        builder.append(" -------+-------------+---------+------------+--------\n");
        for ( Process process : map.values() ) {
            builder.append(String.format("%7s", process.getPid()));
            builder.append(" |");
            builder.append(String.format("%12d", process.getThreadCountMax()));
            builder.append(" |");
            builder.append(String.format("%7.1f%%", process.getCpu()));
            builder.append(" |");
            builder.append(String.format("%8d kB", process.getMemoryMax()));
            builder.append(" |");
            builder.append(String.format(" %s", process.getCmd()));
            builder.append("\n");
        }
        return builder.toString();
    }

    public void printResult() {
        if ( map.isEmpty() )
            return;
        System.out.print("[LAUNCHER:PROFILING] Result:\n\n" + toString() + "\n");
    }

    private static class Process
    {
        private int count;
        private String pid;
        private List<Integer> threadCount = new ArrayList<Integer>();
        private List<Double> cpu = new ArrayList<Double>();
        private List<Integer> memory = new ArrayList<Integer>();
        private String cmd;

        public Process()
        {
            count = 1;
        }

        public Process(String[] values)
        {
            count = 1;
            pid = values[0];
            try {
                threadCount.add(Integer.parseInt(values[1]));
                cpu.add(Double.parseDouble(values[2]));
                memory.add(Integer.parseInt(values[3]));
            } catch ( NumberFormatException e) {
                return;
            }
            cmd = values[4];
        }

        public String getPid() {
            return pid;
        }

        public int getThreadCount() {
            return (int)avg(threadCount);
        }

        public int getThreadCountMax() {
            return max(threadCount);
        }

        public double getCpu() {
            return avg(cpu);
        }

        public int getMemory() {
            return (int)avg(memory);
        }

        public Object getMemoryMax() {
            return max(memory);
        }

        public String getCmd() {
            return cmd;
        }

        public void merge(Process process) {
            count += process.count;
            threadCount.addAll(process.threadCount);
            cpu.addAll(process.cpu);
            memory.addAll(process.memory);
        }

        public static <T extends Comparable> T min(List<T> array){
            T min = array.get(0);
            for ( int i = 0; i < array.size() ; i++ ){
                if ( min.compareTo(array.get(i)) < 0 )
                    min = array.get(i);
            }
            return min;
        }

        public static <T extends Comparable> T max(List<T> array){
            T max = array.get(0);
            for ( int i = 0; i < array.size() ; i++ ){
                if ( max.compareTo(array.get(i)) > 0 )
                    max = array.get(i);
            }
            return max;
        }

        public static <T extends Number> double avg(List<T> array){
            double sum = array.get(0).doubleValue();
            for ( int i = 1; i < array.size() ; i++ ){
                sum += array.get(i).doubleValue();
            }
            return sum / array.size();
        }
    }
}
