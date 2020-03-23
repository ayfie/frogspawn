/*
 * Copyright Florian Schaefer 2019.
 * SPDX-License-Identifier: Apache-2.0
 */

package net.adeptropolis.metis.clustering.postprocessing;

import it.unimi.dsi.fastutil.ints.IntIterators;
import net.adeptropolis.metis.clustering.Cluster;
import net.adeptropolis.metis.graphs.Graph;
import net.adeptropolis.metis.graphs.GraphTestBase;
import net.adeptropolis.metis.graphs.implementations.CompressedSparseGraphBuilder;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.*;

public class ParentSimilarityPostprocessorTest extends GraphTestBase {

  private Graph defaultGraph;
  private Cluster c0;
  private Cluster c1;
  private Cluster c2;
  private Cluster c3;
  private Cluster c4;
  private Cluster c5;
  private Cluster c67;

  @Before
  public void setUp() {
    defaultGraph = new CompressedSparseGraphBuilder()
            .add(0, 1, 1)
            .add(0, 2, 1)
            .add(2, 3, 1)
            .add(2, 4, 1)
            .add(4, 5, 1)
            .add(4, 6, 1)
            .add(4, 7, 1)
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
    c67 = new Cluster(c4);
    c67.addToRemainder(IntIterators.wrap(new int[]{2, 6, 7}));
  }

  @Test
  public void skipIfRootNode() {
    Graph graph = completeGraph(10);
    Cluster root = new Cluster(null);
    root.addToRemainder(IntIterators.wrap(new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}));
    Postprocessor pp = new ParentSimilarityPostprocessor(0.0, 15, graph);
    assertFalse(pp.apply(root));
  }

  @Test
  public void skipIfParentIsRootNode() {
    Graph graph = completeGraph(10);
    Cluster root = new Cluster(null);
    root.addToRemainder(IntIterators.wrap(new int[]{4, 5, 6, 7, 8, 9}));
    Cluster child = new Cluster(root);
    child.addToRemainder(IntIterators.wrap(new int[]{0, 1, 2, 3}));
    Postprocessor pp = new ParentSimilarityPostprocessor(0.0, 15, graph);
    assertFalse(pp.apply(child));
  }

  @Test
  public void thresholdAboveMinOverlap() {
    Postprocessor pp = new ParentSimilarityPostprocessor(0.5, 15, defaultGraph);
    boolean modified = pp.apply(c67);
    assertFalse(modified);
    assertThat(c67.getParent(), is(c4));
    assertThat(c4.getChildren(), hasItem(c67));
  }

  @Test
  @Ignore("Fixme!")
  public void thresholdAllowsPullingUpC67OneLevel() {
    Postprocessor pp = new ParentSimilarityPostprocessor(0.51, 15, defaultGraph);
    boolean modified = pp.apply(c67);
    assertTrue(modified);
    assertThat(c67.getParent(), is(c2));
    assertThat(c2.getChildren(), hasItem(c67));
  }

  @Test
  public void thresholdAllowsPullingUpC67TwoLevels() {
    Postprocessor pp = new ParentSimilarityPostprocessor(0.7, 15, defaultGraph);
    boolean modified = pp.apply(c67);
    assertTrue(modified);
    assertThat(c67.getParent(), is(c0));
    assertThat(c0.getChildren(), hasItem(c67));
  }

  @Test
  public void pullIngUpStopsAtRootNode() {
    Postprocessor pp = new ParentSimilarityPostprocessor(1.0, 15, defaultGraph);
    boolean modified = pp.apply(c67);
    assertTrue(modified);
    assertThat(c67.getParent(), is(c0));
    assertThat(c0.getChildren(), hasItem(c67));
  }


}