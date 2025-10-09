package com.expense.ml;

import weka.core.DenseInstance;
import weka.core.Instances;

public class WekaHelper {
    public static Instances makeInstanceFromRawHeader(Instances rawHeader, String description) {
        Instances inst = new Instances(rawHeader, 0);
        DenseInstance d = new DenseInstance(inst.numAttributes());
        d.setValue(inst.attribute(0), description);
        d.setDataset(inst);
        inst.add(d);
        return inst;
    }
}

