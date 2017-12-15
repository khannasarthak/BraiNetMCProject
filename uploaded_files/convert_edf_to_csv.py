import pyedflib
import numpy as np
import sys
import pdb
from os import listdir
from os.path import isfile, join

onlyfiles = [f for f in listdir('D:\Academic\ASU\Sem 3\Mobile Computing\project\data') if
             isfile(join('D:\Academic\ASU\Sem 3\Mobile Computing\project\data', f))]

for file in onlyfiles[1:]:
    filename_test = file.split('.')[0] + '_test.csv'
    filename_train = file.split('.')[0] + '_train.csv'
    # pdb.set_trace()
    f = pyedflib.EdfReader(file)
    signal_labels = f.getSignalLabels()
    n = f.signals_in_file
    sigbufs = np.zeros((n, f.getNSamples()[0]))
    for i in np.arange(n):
        sigbufs[i, :] = f.readSignal(i)
    # pdb.set_trace()
    # sigbufs = np.transpose(sigbufs)
    sigbufs_test = np.hsplit(sigbufs, 2)[0]
    sigbufs_test = sigbufs_test.take([0], axis=0)
    sigbufs_train = np.hsplit(sigbufs, 2)[1]
    sigbufs_train = sigbufs_train.take([0], axis=0)
    np.savetxt(filename_test, sigbufs_test, delimiter=",")
    np.savetxt(filename_train, sigbufs_train, delimiter=",")
