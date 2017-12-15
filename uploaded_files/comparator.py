import pandas as pd
import numpy as np
import pdb
from sklearn import svm
import pickle
import sys


def svm_train():
    print "########## in svm train ###########"
    train_labels = []
    full_train_data = np.empty([0, 1])
    # full_train_data = pd.DataFrame()
    for i in range(1, 11):
        if i < 10:
            filename = "S00" + str(i) + "R14_train.csv"
        else:
            filename = "S0" + str(i) + "R14_train.csv"
        train_data = pd.read_csv(filename, sep=',', header=None)
        train_data_transpose = train_data.T
        labels = [i] * len(train_data.columns)
        train_labels += labels
        # print "train data ------------>"
        # print train_data_transpose
        # print "full train data --------->"
        # print full_train_data
        # pdb.set_trace()
        # train_array = train_data_transpose.as_matrix()
        # train_fft = fft(train_array)
        # train_fft = train_fft.real
        # full_train_data = full_train_data.append(train_data_transpose, ignore_index=True)
        # pdb.set_trace()
        full_train_data = np.concatenate((full_train_data, train_data_transpose))
    pdb.set_trace()

    model = svm.SVC()
    model.fit(full_train_data, train_labels)
    model_file = 'svm_model_1.sav'
    pickle.dump(model, open(model_file, 'wb'))


def svm_test(test_file):
    # print "########## in svm_test ############"
    model_file = 'svm_model_1.sav'
    loaded_model = pickle.load(open(model_file, 'rb'))
    test_data = pd.read_csv(test_file, sep=',', header=None)
    filename = test_file.split('/')[-1]
    #print "filename: ", filename
    temp_idx = filename[2]
    if temp_idx == '0':
        user_num = int(filename[3])
    else:
        user_num = 10
    test_data_transpose = test_data.T
    # pdb.set_trace()
    result = loaded_model.predict(test_data_transpose)
#    print "user num: ", user_num
    count = (result == user_num).sum()
#    print "count: ", count
    perc = float(count)/float(len(result)) * 100
    if perc > 10:
        return 0
    else:
        return 1


if __name__ == "__main__":
    result = svm_test(sys.argv[1])
#    print result
    exit(result)
