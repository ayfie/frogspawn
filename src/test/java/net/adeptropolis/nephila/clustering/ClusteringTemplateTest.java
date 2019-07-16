package net.adeptropolis.nephila.clustering;

import net.adeptropolis.nephila.graph.implementations.CSRStorage;
import net.adeptropolis.nephila.graph.implementations.CSRStorageBuilder;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

public class ClusteringTemplateTest {

  @Test
  public void emptyPartitionScores() {
    verifyK3_3SubsetScores(new int[]{}, new double[]{});
  }

  @Test
  public void singletonPartitionScores() {
    verifyK3_3SubsetScores(new int[]{5}, new double[]{0});
  }

  @Test
  public void fullGraphScores() {
    verifyK3_3SubsetScores(new int[]{0, 1, 2, 3, 4, 5}, new double[]{1, 1, 1, 1, 1, 1});
  }

  @Test
  public void singleEdgePartitionScores() {
    verifyK3_3SubsetScores(new int[]{1, 5}, new double[]{ 17.0 / (2 + 7 + 17), 17.0 / (17 + 19 + 23)});
  }

  private void verifyK3_3SubsetScores(int[] childIndices, double[] expectedScores) {
    assertThat("Indices size equals expected size", childIndices.length, is(expectedScores.length));
    CSRStorage graph = new CSRStorageBuilder()
            .addSymmetric(0, 1, 2)
            .addSymmetric(0, 2, 3)
            .addSymmetric(0, 3, 5)
            .addSymmetric(4, 1, 7)
            .addSymmetric(4, 2, 11)
            .addSymmetric(4, 3, 13)
            .addSymmetric(5, 1, 17)
            .addSymmetric(5, 2, 19)
            .addSymmetric(5, 3, 23)
            .build();
    ClusteringTemplate template = new ClusteringTemplate(graph);
    double[] scores = template.globalOverlap(graph.view(childIndices));
    assertThat("Score array has correct length", scores.length, is(childIndices.length));
    for (int i = 0; i < expectedScores.length; i++)
      assertThat("Score matches", scores[i], closeTo(expectedScores[i], 1E-6));
  }

}