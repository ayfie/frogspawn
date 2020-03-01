/*
 * Copyright Florian Schaefer 2019.
 * SPDX-License-Identifier: Apache-2.0
 */

package net.adeptropolis.metis.clustering.postprocessing;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.IntIterators;
import net.adeptropolis.metis.clustering.Cluster;
import net.adeptropolis.metis.graphs.Graph;
import net.adeptropolis.metis.graphs.implementations.CompressedSparseGraphBuilder;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class ConsistencyGuardingPostprocessorTest {

  private Graph graph;
  private ConsistencyGuardingPostprocessor unsafeConsistencyGuardingPostprocessor;

  private Cluster c0;
  private Cluster c1;
  private Cluster c2;
  private Cluster c3;
  private Cluster c4;
  private Cluster c5;
  private Cluster c678;
  private Cluster c9;

  @Before
  public void setUp() {
    graph = new CompressedSparseGraphBuilder()
            .add(0, 1, 1)
            .add(0, 2, 1)
            .add(2, 3, 1)
            .add(2, 4, 1)
            .add(4, 5, 1)
            .add(4, 6, 0.6)
            .add(4, 7, 1)
            .add(4, 8, 1)
            .add(6, 7, 1)
            .add(6, 0, 1)
            .add(6, 1, 1)
            .add(6, 2, 1)
            .add(6, 3, 1)
            .add(6, 5, 1)
            .add(6, 7, 1)
            .add(7, 0, 1)
            .add(7, 1, 1)
            .add(7, 2, 1)
            .add(7, 3, 1)
            .add(7, 5, 1)
            .add(8, 7, 10)
            .build();
    c0 = new Cluster(null);
    c0.addToRemainder(IntIterators.wrap(new int[]{0}));
    c1 = new Cluster(c0);
    c1.addToRemainder(IntIterators.wrap(new int[]{1}));
    c2 = new Cluster(c0);
    c2.addToRemainder(IntIterators.wrap(new int[]{2}));
    c3 = new Cluster(c2);
    c3.addToRemainder(IntIterators.wrap(new int[]{3}));
    c4 = new Cluster(c2);
    c4.addToRemainder(IntIterators.wrap(new int[]{4}));
    c5 = new Cluster(c4);
    c5.addToRemainder(IntIterators.wrap(new int[]{5}));
    c678 = new Cluster(c4);
    c678.addToRemainder(IntIterators.wrap(new int[]{6, 7, 8}));
    c9 = new Cluster(c678);
  }

  @Test
  public void ignoreRootCluster() {
    unsafeConsistencyGuardingPostprocessor = new ConsistencyGuardingPostprocessor(graph, 10, 0.5);
    assertFalse(unsafeConsistencyGuardingPostprocessor.apply(c0));
  }

  @Test
  public void allVerticesAreInconsistent() {
    unsafeConsistencyGuardingPostprocessor = new ConsistencyGuardingPostprocessor(graph, 10000, 1.0);
    assertTrue(unsafeConsistencyGuardingPostprocessor.apply(c678));
    assertThat(c4.getChildren(), is(ImmutableSet.of(c5, c9)));
    assertThat(c5.getParent(), is(c4));
    assertThat(c4.getRemainder(), containsInAnyOrder(4, 6, 7, 8));
  }

  @Test
  public void allVerticesAreConsistent() {
    unsafeConsistencyGuardingPostprocessor = new ConsistencyGuardingPostprocessor(graph, 1, 0.0);
    assertFalse(unsafeConsistencyGuardingPostprocessor.apply(c678));
    assertThat(c4.getChildren(), is(ImmutableSet.of(c5, c678)));
    assertThat(c4.getRemainder(), containsInAnyOrder(4));
  }

  @Test
  public void someVerticesAreConsistent() {
    unsafeConsistencyGuardingPostprocessor = new ConsistencyGuardingPostprocessor(graph, 1, 0.27);
    assertTrue(unsafeConsistencyGuardingPostprocessor.apply(c678));
    assertThat(c4.getChildren(), is(ImmutableSet.of(c5, c678)));
    assertThat(c4.getRemainder(), containsInAnyOrder(4, 6));
    assertThat(c678.getRemainder(), containsInAnyOrder(7, 8));
  }

  @Test
  public void numberOfConsistentVerticesBelowMinClusterSize() {
    unsafeConsistencyGuardingPostprocessor = new ConsistencyGuardingPostprocessor(graph, 3, 0.27);
    assertTrue(unsafeConsistencyGuardingPostprocessor.apply(c678));
    assertThat(c4.getChildren(), is(ImmutableSet.of(c5, c9)));
    assertThat(c4.getRemainder(), containsInAnyOrder(4, 6, 7, 8));
  }

}