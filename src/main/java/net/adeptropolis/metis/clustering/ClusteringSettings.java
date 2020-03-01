/*
 * Copyright (c) Florian Schaefer 2020.
 * SPDX-License-Identifier: Apache-2.0
 */

package net.adeptropolis.metis.clustering;

import net.adeptropolis.metis.graphs.Graph;
import net.adeptropolis.metis.graphs.algorithms.power_iteration.ConstantSigTrailConvergence;
import net.adeptropolis.metis.graphs.algorithms.power_iteration.PartialConvergenceCriterion;

/**
 * Stores all relevant clustering settings
 */

public class ClusteringSettings {

  private final ConsistencyMetric consistencyMetric;
  private final int minClusterSize;
  private final double minClusterLikelihood;
  private final double minAncestorOverlap;
  private final int trailSize;
  private final double convergenceThreshold;
  private final int maxIterations;

  /**
   * Constructor
   *
   * @param consistencyMetric    Vertex/cluster consistency metric to be used
   * @param minClusterSize       Minimum cluster size
   * @param minClusterLikelihood Minimum cluster likelihood of a vertex
   * @param minAncestorOverlap   Minimum ancestor overlap of a child cluster node wrt. to its parent
   * @param trailSize            Window size for constant trail convergence (Number of iterations where a vertex must not change its sign)
   * @param convergenceThreshold Fraction of converged vertices
   * @param maxIterations        Maximum number of iterations
   */

  private ClusteringSettings(ConsistencyMetric consistencyMetric, int minClusterSize, double minClusterLikelihood,
                            double minAncestorOverlap, int trailSize, double convergenceThreshold, int maxIterations) {
    this.consistencyMetric = consistencyMetric;
    this.minClusterSize = minClusterSize;
    this.minClusterLikelihood = minClusterLikelihood;
    this.minAncestorOverlap = minAncestorOverlap;
    this.trailSize = trailSize;
    this.convergenceThreshold = convergenceThreshold;
    this.maxIterations = maxIterations;
  }

  /**
   * @return Minumum cluster size
   */

  public int getMinClusterSize() {
    return minClusterSize;
  }

  /**
   * @return Minimum cluster likelihood of a vertex
   */

  public double getMinClusterLikelihood() {
    return minClusterLikelihood;
  }

  /**
   * @return Max allowed number of power method iterations
   */

  public int getMaxIterations() {
    return maxIterations;
  }

  /**
   * Return a new instance of ConvergenceCriterion for a given graph.
   * Currently, this always returns an instance of <code>ConstantSigTrailConvergence</code>
   *
   * @param graph A graph
   * @return A new <code>PartialConvergenceCriterion</code> instance
   */

  public PartialConvergenceCriterion convergenceCriterionForGraph(Graph graph) {
    return new ConstantSigTrailConvergence(graph, trailSize, convergenceThreshold);
  }

  /**
   * @return Minimum ancestor overlap of a child cluster node wrt. to its parent
   */

  public double getMinAncestorOverlap() {
    return minAncestorOverlap;
  }

  /**
   * @return Currently used consistency metric
   */

  public ConsistencyMetric getConsistencyMetric() {
    return consistencyMetric;
  }

  /**
   * Convenience method
   * @return A new builder
   */

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private ConsistencyMetric consistencyMetric = new RelativeWeightConsistencyMetric();
    private int minClusterSize = 50;
    private double minClusterLikelihood = 0.1;
    private double minAncestorOverlap = 0.4;
    private int trailSize = 25;
    private double convergenceThreshold = 0.95;
    private int maxIterations = 10000;

    /**
     * Set consistency metric. Default is <code>RelativeWeightConsistencyMetric</code>
     * @param metric Metric
     * @return this
     */

    public Builder withConsistencyMetric(ConsistencyMetric metric) {
      this.consistencyMetric = metric;
      return this;
    }

    /**
     * Set minimum cluster size. Default is 50
     * @param minClusterSize Minimum cluster size
     * @return this
     */

    public Builder withMinClusterSize(int minClusterSize) {
      this.minClusterSize = minClusterSize;
      return this;
    }

    /**
     * Set minimum cluster likelihood. Default is 0.1
     * @param minClusterLikelihood Minimum likelihood
     * @return this
     */

    public Builder withMinClusterLikelihood(double minClusterLikelihood) {
      this.minClusterLikelihood = minClusterLikelihood;
      return this;
    }

    /**
     * Set Minimum ancestor overlap. Default is 0.4
     * @param minAncestorOverlap Minimum ancestor overlap
     * @return this
     */

    public Builder withMinAncestorOverlap(double minAncestorOverlap) {
      this.minAncestorOverlap = minAncestorOverlap;
      return this;
    }

    /**
     * Set trail size of convergence criterion. Default is 25
     * @param trailSize Trail size
     * @return this
     */

    public Builder withTrailSize(int trailSize) {
      this.trailSize = trailSize;
      return this;
    }

    /**
     * Set convergence threshold (fraction of vertices that have already settled into a cluster). Default is 0.95
     * @param convergenceThreshold Convergene threshold
     * @return this
     */

    public Builder withConvergenceThreshold(double convergenceThreshold) {
      this.convergenceThreshold = convergenceThreshold;
      return this;
    }

    /**
     * Set maxmimum number of iterations for the power method
     * @param maxIterations Maximum number of iterations
     * @return this
     */
    public Builder withMaxIterations(int maxIterations) {
      this.maxIterations = maxIterations;
      return this;
    }

    /**
     * Build settings
     * @return A new instance of <code>ClusteringSettings</code>
     */

    public ClusteringSettings build() {
      return new ClusteringSettings(consistencyMetric, minClusterSize, minClusterLikelihood, minAncestorOverlap,
              trailSize, convergenceThreshold, maxIterations);
    }

  }
}