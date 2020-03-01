/*
 * Copyright Florian Schaefer 2019.
 * SPDX-License-Identifier: Apache-2.0
 */

package net.adeptropolis.metis.graphs.implementations;

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntIterators;
import net.adeptropolis.metis.graphs.Graph;
import net.adeptropolis.metis.graphs.VertexIterator;
import net.adeptropolis.metis.graphs.implementations.arrays.InterpolationSearch;
import net.adeptropolis.metis.graphs.traversal.EdgeConsumer;
import net.adeptropolis.metis.graphs.traversal.ParallelEdgeOps;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Induced subgraph.
 * <p>That is, a graph whose vertex set is limited to a subset of another graph.
 * The edge set is restricted to those edges where both endpoints are members of the given vertex set.</p>
 */

public class CompressedInducedSparseSubgraph extends Graph {

  private final CompressedSparseGraphDatastore datastore;
  private final int[] vertices;
  private long cachedNumEdges = -1L;

  /**
   * Constructor
   *
   * @param datastore The underlying graph datastore
   * @param vertices  An iterator of global vertex ids
   */

  CompressedInducedSparseSubgraph(CompressedSparseGraphDatastore datastore, IntIterator vertices) {
    this.datastore = datastore;
    this.vertices = IntIterators.unwrap(vertices);
    Arrays.parallelSort(this.vertices, 0, order());
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public int order() {
    return vertices.length;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public long size() {
    if (cachedNumEdges >= 0) {
      return cachedNumEdges;
    } else {
      EdgeCountingConsumer edgeCountingConsumer = new EdgeCountingConsumer();
      traverseParallel(edgeCountingConsumer);
      cachedNumEdges = edgeCountingConsumer.getCount();
      return cachedNumEdges;
    }
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public VertexIterator vertexIterator() {
    return new SubgraphVertexIterator().reset(vertices);
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public int[] collectVertices() {
    return vertices.clone();
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public void traverseParallel(EdgeConsumer consumer) {
    ParallelEdgeOps.traverse(this, consumer);
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public void traverseParallel(int v, EdgeConsumer consumer) {

    if (order() == 0 || v < 0) {
      return;
    }

    int globalId = globalVertexId(v);

    long low = datastore.pointers[globalId];
    long high = datastore.pointers[globalId + 1];

    if (low == high) {
      return;
    }

    if (order() > high - low) {
      traverseByAdjacent(v, consumer, low, high);
    } else {
      traverseByVertices(v, consumer, low, high);
    }
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public int localVertexId(int globalVertexId) {
    return InterpolationSearch.search(vertices, globalVertexId, 0, order() - 1);
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public int globalVertexId(int localVertexId) {
    return vertices[localVertexId];
  }

  /**
   * Internal: Traverse all neighbours of a given local vertex by the non-zero entries of the adjacency matrix
   *
   * @param leftEndpoint A local vertex id
   * @param consumer     An instance of <code>EdgeConsumer</code>
   * @param low          Initial edge pointer
   * @param high         Maximum edge pointer (inclusive!)
   */

  private void traverseByAdjacent(final int leftEndpoint, final EdgeConsumer consumer, final long low, final long high) {
    int secPtr = 0;
    int rightEndpoint;
    for (long ptr = low; ptr < high; ptr++) {
      rightEndpoint = InterpolationSearch.search(vertices, datastore.edges.get(ptr), secPtr, order() - 1);
      if (rightEndpoint >= 0) {
        consumer.accept(leftEndpoint, rightEndpoint, datastore.weights.get(ptr));
        secPtr = rightEndpoint + 1;
      }
      if (secPtr >= order()) break;
    }
  }

  /**
   * Traverse all neighbours of a given local vertex by the vertex set
   *
   * @param leftEndpoint A local vertex id
   * @param consumer     An instance of <code>EdgeConsumer</code>
   * @param low          Initial edge pointer
   * @param high         Maximum edge pointer (inclusive!)
   */

  private void traverseByVertices(final int leftEndpoint, final EdgeConsumer consumer, final long low, final long high) {
    long ptr = low;
    long retrievedIdx;
    for (int i = 0; i < order(); i++) {
      retrievedIdx = InterpolationSearch.search(datastore.edges, vertices[i], ptr, high - 1);
      if (retrievedIdx >= 0 && retrievedIdx < high) {
        consumer.accept(leftEndpoint, i, datastore.weights.get(retrievedIdx));
        ptr = retrievedIdx + 1;
      }
      if (ptr >= high) break;
    }
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public Graph inducedSubgraph(IntIterator vertices) {
    return new CompressedInducedSparseSubgraph(datastore, vertices);
  }

  /**
   * Consumer counting the total number of distinct edges of the graph
   */

  private static class EdgeCountingConsumer implements EdgeConsumer {

    private final AtomicLong cnt;

    public EdgeCountingConsumer() {
      cnt = new AtomicLong();
    }

    @Override
    public void accept(int u, int v, double weight) {
      cnt.incrementAndGet();
    }

    public long getCount() {
      return cnt.get();
    }

  }
}