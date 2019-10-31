package net.adeptropolis.nephila.graphs;

public class VertexWeights implements EdgeConsumer {

  private final Graph graph;

  private final double[] weights;

  private VertexWeights(Graph graph) {
    this.graph = graph;
    this.weights = new double[graph.size()];
  }

  public static double[] compute(Graph graph) {
    VertexWeights weights = new VertexWeights(graph);
    EdgeOps.traverse(graph, weights);
    return weights.weights;
  }

  @Override
  public void accept(int u, int v, double weight) {
    weights[u] += weight;
  }

}