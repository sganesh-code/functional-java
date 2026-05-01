package io.github.senthilganeshs.fj.ds;

import org.openjdk.jmh.annotations.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@org.openjdk.jmh.annotations.State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
public class ComprehensiveCollectionsBenchmark {

    @Param({"1000"})
    int size;

    java.util.List<Integer> testData;
    
    // FJ Collections
    List<Integer> fjList;
    Vector<Integer> fjVector;
    HashMap<Integer, Integer> fjHashMap;
    Set<Integer> fjSet;
    RoseTree<Integer> fjRoseTree;
    PriorityQueue<Integer> fjPriorityQueue;

    // Java Collections
    java.util.List<Integer> javaArrayList;
    java.util.Map<Integer, Integer> javaHashMap;
    java.util.Set<Integer> javaTreeSet;
    java.util.PriorityQueue<Integer> javaPriorityQueue;

    @Setup
    public void setup() {
        testData = new ArrayList<>(size);
        Random rnd = new Random(42);
        for (int i = 0; i < size; i++) {
            testData.add(rnd.nextInt(size));
        }
        
        // FJ
        fjList = List.of(testData);
        fjVector = Vector.of(testData.toArray(new Integer[0]));
        fjHashMap = HashMap.nil();
        for (int i = 0; i < size; i++) {
            fjHashMap = fjHashMap.put(i, testData.get(i));
        }
        fjSet = Set.of(testData.toArray(new Integer[0]));
        fjRoseTree = RoseTree.of(0);
        for (Integer i : testData) fjRoseTree = (RoseTree<Integer>) fjRoseTree.build(i);
        fjPriorityQueue = PriorityQueue.of(testData.toArray(new Integer[0]));

        // Java
        javaArrayList = new ArrayList<>(testData);
        javaHashMap = new java.util.HashMap<>();
        for (int i = 0; i < size; i++) {
            javaHashMap.put(i, testData.get(i));
        }
        javaTreeSet = new java.util.TreeSet<>(testData);
        javaPriorityQueue = new java.util.PriorityQueue<>(testData);
    }

    // --- CONSTRUCTION ---

    @Benchmark
    public List<Integer> fj_buildList() {
        List<Integer> list = List.nil();
        for (Integer i : testData) list = list.build(i);
        return list;
    }

    @Benchmark
    public java.util.List<Integer> java_buildArrayList() {
        java.util.List<Integer> list = new ArrayList<>();
        for (Integer i : testData) list.add(i);
        return list;
    }

    @Benchmark
    public Vector<Integer> fj_buildVector() {
        Vector<Integer> v = Vector.nil();
        for (Integer i : testData) v = (Vector<Integer>) v.build(i);
        return v;
    }

    @Benchmark
    public HashMap<Integer, Integer> fj_buildHashMap() {
        HashMap<Integer, Integer> map = HashMap.nil();
        for (int i = 0; i < size; i++) map = map.put(i, testData.get(i));
        return map;
    }

    @Benchmark
    public java.util.Map<Integer, Integer> java_buildHashMap() {
        java.util.Map<Integer, Integer> map = new java.util.HashMap<>();
        for (int i = 0; i < size; i++) map.put(i, testData.get(i));
        return map;
    }

    // --- RANDOM ACCESS / LOOKUP ---

    @Benchmark
    public Maybe<Integer> fj_vectorAccess() {
        return fjVector.at(size / 2);
    }

    @Benchmark
    public Integer java_arrayListAccess() {
        return javaArrayList.get(size / 2);
    }

    @Benchmark
    public Maybe<Integer> fj_hashMapLookup() {
        return fjHashMap.get(size / 2);
    }

    @Benchmark
    public Integer java_hashMapLookup() {
        return javaHashMap.get(size / 2);
    }

    @Benchmark
    public boolean fj_setContains() {
        return fjSet.contains(testData.get(size / 2));
    }

    @Benchmark
    public boolean java_treeSetContains() {
        return javaTreeSet.contains(testData.get(size / 2));
    }

    // --- SEQUENTIAL PROCESSING ---

    @Benchmark
    public Integer fj_foldlList() {
        return fjList.foldl(0, (acc, i) -> acc + i);
    }

    @Benchmark
    public Integer java_streamReduceList() {
        return javaArrayList.stream().reduce(0, Integer::sum);
    }

    // --- QUEUE ---

    @Benchmark
    public Queue<Integer> fj_queueMixedOps() {
        Queue<Integer> q = Queue.nil();
        for (Integer i : testData) q = (Queue<Integer>) q.build(i);
        for (int i = 0; i < size; i++) q = q.dequeue().orElse(Tuple.of(0, q)).getB().orElse(Queue.nil());
        return q;
    }

    @Benchmark
    public java.util.Deque<Integer> java_arrayDequeOps() {
        java.util.Deque<Integer> q = new java.util.ArrayDeque<>();
        for (Integer i : testData) q.addLast(i);
        for (int i = 0; i < size; i++) q.removeFirst();
        return q;
    }

    @Benchmark
    public PriorityQueue<Integer> fj_priorityQueueOps() {
        PriorityQueue<Integer> pq = PriorityQueue.nil();
        for (Integer i : testData) pq = (PriorityQueue<Integer>) pq.build(i);
        for (int i = 0; i < size; i++) pq = pq.deleteMin().orElse(PriorityQueue.nil());
        return pq;
    }

    @Benchmark
    public java.util.PriorityQueue<Integer> java_priorityQueueOps() {
        java.util.PriorityQueue<Integer> pq = new java.util.PriorityQueue<>();
        for (Integer i : testData) pq.add(i);
        for (int i = 0; i < size; i++) pq.poll();
        return pq;
    }

    @Benchmark
    public Integer fj_roseTreeFold() {
        return fjRoseTree.foldl(0, (acc, i) -> acc + i);
    }
}
