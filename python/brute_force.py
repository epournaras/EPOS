import numpy as np
import time
import multiprocessing as mp
import os
import sys, getopt

from dataset import get_raw_data

FLAGS = None
users_choices = None
dataset = None
folder_name = None

__author__ = 'Thomas Asikis'
__institute__ = 'ETH Zurich'
__department__ = "Computational Social Science"


def long_range(start, stop):
    '''
    A memory efficient generator of a 64bit long max size range
    :param start: range start integer
    :param stop: range end integer, exclusive
    :return:
    '''
    i = start
    while i < stop:
       yield i
       i += 1


def get_sum_variance(indeces, dataset):
    '''
    Calculcates the variance of given selected plan indeces, given the dataset
    :param indeces: the indeces for the plans for each agent
    :param dataset: the dataset of dimensions (agent, plan, timestep)
    :return:
    '''
    shape = np.shape(dataset)
    sum_a = np.zeros(shape[2])
    for i in range(shape[0]):
        sum_a = np.add(sum_a, dataset[i, indeces[i], :])

    var = np.var(sum_a, ddof=1)
    return var


def index_job(i):
    '''
    A job for a certain index. Converts the index to a plan selection from all the agents
    via linear indexing.
    :param i:
    :return: The String with the plan selection for all agents and also the variance of the plan
    '''
    index = np.unravel_index(i, users_choices)
    var = get_sum_variance(index, dataset)
    index_s = str(index).replace("(","").replace(")","").replace(" ","")
    var_s = str(var)
    result = "".join((index_s, "::",var_s, "\n" ))
    return result, var


def range_job(bounds):
    '''
    A job for certain range that evaluates all the elements within bounds
    :param bounds:
    :return:
    '''
    #result = ""
    with open(folder_name+str(bounds[0])+"-"+str(bounds[1])+".txt", 'a') as appendFile:
        bvar = None
        for i in range(bounds[0], bounds[1]):
            res, var = index_job(i)
            bvar = bvar or var
            bvar = var if var < bvar else bvar
            appendFile.write(res)
        print(bvar)


def ranges(start, end, number):
    '''
    Splits a range to sub-ranges
    :param start: the start of the range
    :param end: the end of the range
    :param number: number of splits
    :return: a list of all the sub-ranges
    '''
    N = end - start
    step = N // number
    return list((round(step*i)+start, round(step*(i+1)+start)) for i in range(number))


def calc_user_choices(ds):
    '''
    Calculate all possible combinations of user plan selections. Be aware that for a large number
    of plans and users, this will create overflows. Nevertheless this brute force is not expected to
    finish in real time for over several billions of combinations.
    :param ds:
    :return:
    '''
    shape = np.shape(dataset)
    result = np.array(list(0 for i in range(0, shape[0], 1)))

    for user in range(shape[0]):
        for plan in range(shape[1]):
            c_plan = dataset[user, plan, :]
            #any_non_zeroes = np.any(c_plan)
            all_non_nans = not np.any(np.isnan(c_plan))
            all_finite = not np.any(np.isinf(c_plan))

            if all_non_nans and all_finite:
                result[user] +=1

    return result


def exec_exhaustive(ds, output_folder):
    '''
    Run a parallel exhaustive search and write down all the results in a folder
    :param ds: A numpy array containing a float valued dataset of dimensions [agents, plans, time-steps]
    :param output_folder: the folder, where to store the results of the exhaustive search
    :return: None
    '''

    global dataset
    dataset = ds

    print("dataset shape: " + str(np.shape(dataset)))

    global users_choices
    users_choices = calc_user_choices(dataset)

    print("plans per user: " + str(users_choices))

    global folder_name
    folder_name = output_folder

    start = time.time()

    pool = mp.Pool()

    range_start = 0
    #range_end = plans ** users
    range_end = np.prod(np.array(users_choices))

    print("total combinations: " + str(range_end))

    rs = ranges(range_start, range_end, mp.cpu_count())
    print("combinations for threads: " + str(rs))
    start = time.time()
    pool.map(range_job, ([i[0], i[1]] for i in rs))
    end = time.time()
    diff = end - start
    print("total execution time: " + str(diff))

def main(argv):
    inputfolder = ''
    outputfolder = ''
    try:
        opts, args = getopt.getopt(argv,"hi:o:",["ifolder=","ofolder="])
    except getopt.GetoptError:
        print('test.py -i <inputfolder> -o <outputfolder>')
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-h':
            print('test.py -i <inputfolder> -o <outputfolder>')
            sys.exit()
        elif opt in ("-i", "--ifolder"):
            inputfolder = arg
        elif opt in ("-o", "--ofolder"):
            outputfolder = arg
    print('Input folder is "', inputfolder)
    print('Output folder is "', outputfolder)


    ds = get_raw_data(inputfolder)
    if not os.path.exists(outputfolder):
        os.makedirs(outputfolder)

    exec_exhaustive(ds, outputfolder)

if __name__ == '__main__':
    main(sys.argv[1:])
