/*
 * Copyright (c) Florian Schaefer 2020.
 * SPDX-License-Identifier: Apache-2.0
 */

package net.adeptropolis.metis.clustering;

import com.google.common.base.Preconditions;
import net.adeptropolis.metis.ClusteringSettings;
import net.adeptropolis.metis.clustering.consistency.ConsistencyGuard;
import net.adeptropolis.metis.clustering.postprocessing.Postprocessing;
import net.adeptropolis.metis.graphs.Graph;
import net.adeptropolis.metis.graphs.algorithms.ConnectedComponents;
import net.adeptropolis.metis.graphs.algorithms.SpectralBisector;
import net.adeptropolis.metis.graphs.algorithms.power_iteration.PowerIteration;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * <p>Recursive clustering</p>
 * <p>Take a given graph and return a hierarchy of semantically consistent clusters</p>
 */

public class RecursiveClustering {

  private static final Logger LOG = LoggerFactory.getLogger(RecursiveClustering.class.getSimpleName());

  private final Graph graph;
  private final ClusteringSettings settings;
  private final SpectralBisector bisector;
  private final ConsistencyGuard consistencyGuard;

  // NOTE: By construction, this type of queue induces the top-town ordering required for determinism
  // and ensures the correct behaviour of consistency guards
  private final ConcurrentLinkedQueue<Protocluster> queue;

  /**
   * Constructor
   *
   * @param graph    Input graph
   * @param settings Clustering settings
   */

  public RecursiveClustering(Graph graph, ClusteringSettings settings) {
    this.graph = graph;
    this.settings = settings;
    this.bisector = new SpectralBisector(settings);
    this.queue = new ConcurrentLinkedQueue<>();
    this.consistencyGuard = new ConsistencyGuard(settings.getConsistencyMetric(), graph, settings.getMinClusterSize(), settings.getMinClusterLikelihood());
  }

  /**
   * Run the recursive clustering
   *
   * @return Root cluster of the generated cluster hierarchy
   */

  public Cluster run() {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    Cluster root = new Cluster(null);
    Protocluster initialProtocluster = new Protocluster(graph, Protocluster.GraphType.ROOT, root);
    queue.add(initialProtocluster);
    processQueue();
    stopWatch.stop();
    LOG.debug("Finished clustering {} vertices after {}", graph.order(), stopWatch);
    return new Postprocessing(root, graph, settings).apply();
  }

  /**
   * Process all elements of the recursive clustering task queue until it is exhausted.
   * Depending on type of the protocluster, it is either decomposed into its connected components or subject to
   * spectral bisection.
   */

  private void processQueue() {
    while (!queue.isEmpty()) {
      Protocluster protocluster = queue.poll();
      if (protocluster.getGraphType() == Protocluster.GraphType.COMPONENT) {
        bisect(protocluster);
      } else {
        decomposeComponents(protocluster);
      }
    }
  }

  /**
   * Bisect the protocluster's graph such that the normalized cut is minimized. Possible scenarios for each partition:
   * <ol>
   *   <li>The partition is either smaller than the allowed minimum cluster size or comprises the full
   *   input graph (which hints an error). In that case, add its vertices to the cluster's remainder and terminate</li>
   *   <li>
   *     Ensure consistency of the resulting subgraph vertices and add all non-compliant vertices to the cluster reminder.
   *     For the remaining subgraph, one of two conditions may apply:
   *     <ol>
   *       <li>The remaining consistent subgraph is smaller than the allowed minimum cluster
   *       size → Add its vertices to the cluster's remainder and terminate</li>
   *       <li>Else: Create a new protocluster with graph type <code>SPECTRAL</code> and add it to the queue
   *       (unless it's exactly as large as the minimum cluster size, in which case a new child cluster is created).</li>
   *     </ol>
   *   </li>
   * </ol>
   *
   * @param protocluster A protocluster
   */

  private void bisect(Protocluster protocluster) {
    try {
      bisector.bisect(protocluster.getGraph(), settings.getMaxIterations(), partition -> {
        if (partition.order() < settings.getMinClusterSize() || partition.order() == protocluster.getGraph().order()) {
          protocluster.getCluster().addToRemainder(partition);
        } else {
          Graph consistentSubgraph = consistencyGuard.ensure(protocluster.getCluster(), partition);
          if (consistentSubgraph != null) {
            processConsistentSubgraph(protocluster, consistentSubgraph);
          }
        }
      });
    } catch (PowerIteration.MaxIterationsExceededException e) {
      if (protocluster.getGraph().size() >= settings.getMinClusterSize()) {
        addTerminalChild(protocluster, protocluster.getGraph());
      } else {
        protocluster.getCluster().addToRemainder(protocluster.getGraph());
      }
      LOG.debug("Exceeded maximum number of iterations ({}). Not clustering any further.", settings.getMaxIterations());
    }
  }

  /**
   * <p>Process a subgraph returned by a consistency guard.</p>
   * <p>If the graph is larger than the minimum cluster size, put it back into the processing queue.
   * Otherwise, create a child cluster from its vertices and terminate here.</p>
   *
   * @param protocluster       Protocluster
   * @param consistentSubgraph Subgraph whose vertices are fully self-consistent wrt. to the graph
   */

  private void processConsistentSubgraph(Protocluster protocluster, Graph consistentSubgraph) {
    if (consistentSubgraph.size() > settings.getMinClusterSize()) {
      enqueueProtocluster(Protocluster.GraphType.SPECTRAL, protocluster.getCluster(), consistentSubgraph);
    } else {
      Preconditions.checkState(consistentSubgraph.size() == settings.getMinClusterSize());
      addTerminalChild(protocluster, consistentSubgraph);
    }
  }

  /**
   * Create a new child to the current (proto)cluster and add a graph to its remainder
   *
   * @param protocluster The current protocluster
   * @param graph        A graph
   */

  private void addTerminalChild(Protocluster protocluster, Graph graph) {
    Cluster child = new Cluster(protocluster.getCluster());
    child.addToRemainder(graph);
  }

  /**
   * Decompose the current protocluster's graph into its connected components. There are 4 possible scenarios:
   * <ol>
   *   <li>The input graph was already fully connected. Label the protocluster's graph as connected component and re-add it to the queue</li>
   *   <li>The connected component is smaller than the minimum cluster size. In that case, add its vertices to the cluster's reminder and terminate</li>
   *   <li>The connected component order is exactly the minimum cluster size. Add a new child with the connected component vertices</li>
   *   <li>Else label the component as connected and add a proper new protocluster to the queue</li>
   * </ol>
   *
   * @param protocluster A protocluster
   */

  private void decomposeComponents(Protocluster protocluster) {
    ConnectedComponents.find(protocluster.getGraph(), component -> {
      if (component.order() == protocluster.getGraph().order()) {
        protocluster.setGraphTypeConnectedComponent();
        queue.add(protocluster);
      } else if (component.order() < settings.getMinClusterSize()) {
        protocluster.getCluster().addToRemainder(component);
      } else if (component.order() == settings.getMinClusterSize()) {
        addTerminalChild(protocluster, component);
      } else if (component.order() > settings.getMinClusterSize()) {
        enqueueProtocluster(Protocluster.GraphType.COMPONENT, protocluster.getCluster(), component);
      }
    });
  }

  /**
   * Insert a new protocluster into the queue
   *
   * @param graphType Type of the graph for the new protocluster
   * @param parent    Parent cluster
   * @param subgraph  Protocluster graph
   */

  private void enqueueProtocluster(Protocluster.GraphType graphType, Cluster parent, Graph subgraph) {
    Cluster childCluster = new Cluster(parent);
    Protocluster protocluster = new Protocluster(subgraph, graphType, childCluster);
    queue.add(protocluster);
  }

}