import reader as rdr
import pandas as pd
import numpy as np
__author__ = 'Thomas Asikis'
__institute__ = 'ETH Zurich'
__department__ = "Computational Social Science"


def get_raw_data(folder_path:str, sort_agents:bool = True, shuffle_seed:int=0):
    '''

    :param folder_path: The folder that contains the agent.plans files. The format of the files should be:
    preference_value::timestep_1,timestep_2....
    Where each value is float and each line is a plan.
    :param sort_agents: Sorts agens based on filenames
    :param shuffle_seed: If agent sorting is false, then this seed is to sort in a reproducible random manner
     An initialization method.
     Loads the datas/home/thomaset fromt he config folder. It splits files based
     on the provided EPOS format which is for a line:
     pref_weight(float32):consumption_value_1(float32),...,consumption_value144(float32)
     Uses pandas and numpy for intermediate storage.
     Possible memory inefficiency and extra processing
     :return: A numpy array of the following shape: (plans, time, agents)
    '''
    print("Loading agent data from folder: " + folder_path)
    data_paths = [file for file in rdr.walk_plans_dataset(folder_path, sort_asc=sort_agents, shuffle_seed=shuffle_seed)]
    agent_arrays = []
    i = 0

    max_plan = None
    max_timesteps = None

    for path in data_paths:
        #print("Now loading: " + path)
        frame = pd.read_csv(path, skiprows=0, header=None, #usecols = range(1, num_dimensions[2] + 1), names= col_names,
                            sep=',|:', engine='python')
        frame.drop(0, axis=1, inplace=True)
        np_array = frame.as_matrix()
        shape = np.shape(np_array)
        max_plan = max_plan or shape[0]
        max_plan = shape[0] if shape[0] > max_plan else max_plan
        max_timesteps = max_timesteps or shape[1]
        max_timesteps = shape[1] if shape[1] > max_timesteps else max_timesteps
        #print(currentShape)
        agent_arrays.append(np_array)
        i = i + 1
    raw_data = np.empty((len(data_paths), max_plan, max_timesteps))

    np.set_printoptions(threshold=np.nan)
    for agent in range(len(agent_arrays)):
        plans =  agent_arrays[agent]
        shape_plans = np.shape(plans)
        raw_data[agent, :shape_plans[0], :shape[1]] = plans
        #print(plans)
        #for plan_index in range(shape_plans[0]):
        #raw_data[agent, plan_index, :] = plans[plan_index]

    #print(raw_data)
    #raw_data = np.array(agent_arrays, ndmin=3)

    return raw_data

