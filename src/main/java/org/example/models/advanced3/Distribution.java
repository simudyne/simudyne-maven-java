package org.example.models.advanced3;

import org.apache.commons.math3.random.EmpiricalDistribution;
import simudyne.core.util.SeededRandom;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.DoubleStream;

public class Distribution {
  public class BucketInfo {
    public final int min;
    public final int max;
    public final double p;

    public BucketInfo(int min, int max, double p) {
      this.min = min;
      this.max = max;
      this.p = p;
    }
  }

  private SeededRandom seededRandom = SeededRandom.create(42);

  private List<BucketInfo> bucketInfo = new ArrayList<>();

  public double[] samples;

  public Distribution() {
    bucketInfo.add(new BucketInfo(2200, 2800, 0.000832961));
    bucketInfo.add(new BucketInfo(2800, 3600, 0.006795891));
    bucketInfo.add(new BucketInfo(3600, 4700, 0.010746074));
    bucketInfo.add(new BucketInfo(4700, 6000, 0.012514807));
    bucketInfo.add(new BucketInfo(6000, 7700, 0.033961671));
    bucketInfo.add(new BucketInfo(7700, 9900, 0.056495643));
    bucketInfo.add(new BucketInfo(9900, 12700, 0.061135305));
    bucketInfo.add(new BucketInfo(12700, 16300, 0.081532622));
    bucketInfo.add(new BucketInfo(16300, 21000, 0.094317337));
    bucketInfo.add(new BucketInfo(21000, 26900, 0.116546926));
    bucketInfo.add(new BucketInfo(26900, 34600, 0.117914457));
    bucketInfo.add(new BucketInfo(34600, 44400, 0.126719849));
    bucketInfo.add(new BucketInfo(44400, 57000, 0.107215589));
    bucketInfo.add(new BucketInfo(57000, 73200, 0.075016589));
    bucketInfo.add(new BucketInfo(73200, 94000, 0.047784181));
    bucketInfo.add(new BucketInfo(94000, 120700, 0.050470097));

    int nbSamples = 10000;

    samples =
        bucketInfo
            .stream()
            .flatMapToDouble(
                b -> {
                  int n = (int) (b.p * nbSamples);
                  return DoubleStream.of(seededRandom.uniform(b.min, b.max).sample(n));
                })
            .toArray();
  }

  public EmpiricalDistribution getIncomeDistribution() {
    EmpiricalDistribution empiricalDistribution = new EmpiricalDistribution();
    empiricalDistribution.load(samples);
    return empiricalDistribution;
  }
}
