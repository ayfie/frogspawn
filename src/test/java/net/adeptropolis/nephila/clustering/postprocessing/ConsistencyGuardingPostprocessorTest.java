/*
 * Copyright (c) Florian Schaefer 2019.
 *
 * This file is subject to the terms and conditions defined in the
 * file 'LICENSE.txt', which is part of this source code package.
 */

package net.adeptropolis.nephila.clustering.postprocessing;

import net.adeptropolis.nephila.clustering.Cluster;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class ConsistencyGuardingPostprocessorTest {

  @Test
  public void nothingToDo() {
    ConsistencyGuardingPostprocessor pp = new ConsistencyGuardingPostprocessor(0, 0, null);
    boolean changed = pp.apply(new Cluster(null));
    assertFalse(changed);
  }

}