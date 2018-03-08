import os
import json
import re
import operator
from random import shuffle, seed

__author__ = 'Thomas Asikis'
__institute__ = 'ETH Zurich'
__department__ = "Computational Social Science"


#custom lazy loaders via generators for the files

def walk_directory(root_dir):
    """
    Creates a generator that traverses all the files under the given root directory
    This generator can be consumed only once (one-shot)
    :param root_dir:
    :return:A generator that traverses the directory subtree under the given root.
    Each element is a path (String) pointing to a file
    """
    for root, dirs, files in os.walk(root_dir, topdown=False):
        for name in files:
            yield root+name


def walk_plans_dataset(root_dir, sort_asc = True, shuffle_seed = None):
    '''
    Walks the provided root_dir path and returns all the files with the suffix ".plans".
    By defaulr it returns them as sorted. If sorted is set to false, the random sortings can be achieved by
    changing the Seed value. If seed remains None, then a (pseudo)random sorting is chosen.
    :param root_dir:: the directoru that contains the plans
    :param sorted: default value true, if true returns a list of agents sorted on the first integer found in the file name
    :param seed: the seed for the shuffling. If none a random shuffling is generated
    :return: a shorted or shuffled list of agents
    '''
    result = {}
    for path in walk_directory(root_dir):
        name = os.path.basename(path)
        match = re.search('\d+', name)
        if(name.endswith("plans") and match is not None):
            key = match.group(0)
            result[int(key)]=path
    if (sort_asc):
        sorted_x = sorted(result.items(), key=operator.itemgetter(0))
        for pair in sorted_x:
            yield pair[1]
    else:
        if(shuffle_seed is not None):
            seed(shuffle_seed)
        keys = list(result.keys())
        shuffle(keys)
        for key in keys:
            yield result[key]



def read_file_lines(path):
    """
    A generator that yields the String of each line of a file. One shot.
    :param path: A string representation of a path
    :return: A stream of lines
    """
    for line in open(path):
        yield line


def line_to_json(line):
    """
    loads into a dict object a json string that is parsed in a line.
    Useful for wikipedia dumps and also java/javascript communication via objects.
    :param line: an input line
    :return: a json dictionary object.
    """
    return json.loads(line)


def read_json_lines(root_dir):
    """
    Walks through all files in a directory. Ignores a license.txt file.
    Reads each file and converts each object into a json dict.
    :param root_dir:
    :return: a stream of json dicts
    """
    for file in walk_directory(root_dir):
        if os.path.basename(file) != 'license.txt':
            for line in read_file_lines(file):
                yield line_to_json(line)


