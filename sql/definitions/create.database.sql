\c postgres
DROP DATABASE IF EXISTS epos;
CREATE DATABASE epos;
\c epos ;

--###########
--###########
############
#customlog
--###########
--###########
--###########

DROP TABLE IF EXISTS customlog CASCADE;

CREATE TABLE customlog
(
    -- required fields
	seq_id SERIAL NOT NULL		-- auto-increment field
	,dt TIMESTAMP NOT NULL
    
    ,run NUMERIC NOT NULL 
	,iteration NUMERIC NOT NULL
	,dim_0 NUMERIC NOT NULL
	,dim_1 NUMERIC NOT NULL
	--,dim_... NUMERIC NOT NULL

	
);

--###########
--###########
############
#eventlog
--###########
--###########
--###########

DROP TABLE IF EXISTS eventlog CASCADE;

 --DROP TABLE eventlog_default;
CREATE TABLE eventlog
(
    -- standard DIAS fields
	seq_id SERIAL NOT NULL
	,dt TIMESTAMP NOT NULL
    ,current_time_millis BIGINT NOT NULL -- timestamp in milli-seconds set by the peer
    
    ,network NUMERIC NOT NULL DEFAULT 0 -- the name of the DIAS aggregation network on which the peer is running
    
	,peer NUMERIC NOT NULL
	,epoch BIGINT NOT NULL
    ,peer_event_counter BIGINT NOT NULL -- internal counter of the events inside the peer
    
    -- specific fields
    ,thread_id NUMERIC NOT NULL
    ,classname TEXT NOT NULL
    ,func TEXT NOT NULL
    ,key TEXT
    ,value TEXT
	
);



-- partitioning insert function
CREATE OR REPLACE FUNCTION event_log_partition_insert_trigger() RETURNS TRIGGER AS 
$$ 
DECLARE lcsymbol varchar (6); 
DECLARE target_table varchar; 
DECLARE insert_sql varchar; 
BEGIN  
    IF ( NEW.network <= 10 ) THEN 
        target_table := 'eventlog_' || NEW.network;	
        EXECUTE format('INSERT INTO %I VALUES( $1.* )', target_table) USING NEW; 
    ELSE  
        INSERT INTO eventlog_default VALUES (NEW.*);  
    END IF; 
RETURN NULL; 
END;  
$$ LANGUAGE plpgsql;


-- insert trigger; calls the partitioning insert function
CREATE TRIGGER eventlog_trigger BEFORE INSERT ON eventlog FOR EACH ROW EXECUTE PROCEDURE event_log_partition_insert_trigger();



-- partitions, 1 per peer 
-- first 40 peers get 1 partition each, afterwards data goes into a default table
-- partitions are created for each partition

-- default partition and it0s indexes
CREATE TABLE eventlog_default ( CHECK( network > 10 ) ) INHERITS (eventlog);
CREATE INDEX CONCURRENTLY eventlog_default_seq_idx ON eventlog_default USING BRIN(seq_id);	
CREATE INDEX CONCURRENTLY eventlog_default_epoch_idx ON eventlog_default USING BRIN(epoch);	
CREATE INDEX CONCURRENTLY eventlog_default_date_idx ON eventlog_default USING BTREE (cast(dt as date));

-- partitions for networks 0 to 10
CREATE TABLE eventlog_0 ( CHECK( network = 0) ) INHERITS (eventlog);
CREATE TABLE eventlog_1 ( CHECK( network = 1) ) INHERITS (eventlog);
CREATE TABLE eventlog_2 ( CHECK( network = 2) ) INHERITS (eventlog);
CREATE TABLE eventlog_3 ( CHECK( network = 3) ) INHERITS (eventlog);
CREATE TABLE eventlog_4 ( CHECK( network = 4) ) INHERITS (eventlog);
CREATE TABLE eventlog_5 ( CHECK( network = 5) ) INHERITS (eventlog);
CREATE TABLE eventlog_6 ( CHECK( network = 6) ) INHERITS (eventlog);
CREATE TABLE eventlog_7 ( CHECK( network = 7) ) INHERITS (eventlog);
CREATE TABLE eventlog_8 ( CHECK( network = 8) ) INHERITS (eventlog);
CREATE TABLE eventlog_9 ( CHECK( network = 9) ) INHERITS (eventlog);
CREATE TABLE eventlog_10 ( CHECK( network = 10) ) INHERITS (eventlog);


-- index on seq_id
CREATE INDEX CONCURRENTLY eventlog_0_seq_idx ON eventlog_0 USING BRIN(seq_id);
CREATE INDEX CONCURRENTLY eventlog_1_seq_idx ON eventlog_1 USING BRIN(seq_id);
CREATE INDEX CONCURRENTLY eventlog_2_seq_idx ON eventlog_2 USING BRIN(seq_id);
CREATE INDEX CONCURRENTLY eventlog_3_seq_idx ON eventlog_3 USING BRIN(seq_id);
CREATE INDEX CONCURRENTLY eventlog_4_seq_idx ON eventlog_4 USING BRIN(seq_id);
CREATE INDEX CONCURRENTLY eventlog_5_seq_idx ON eventlog_5 USING BRIN(seq_id);
CREATE INDEX CONCURRENTLY eventlog_6_seq_idx ON eventlog_6 USING BRIN(seq_id);
CREATE INDEX CONCURRENTLY eventlog_7_seq_idx ON eventlog_7 USING BRIN(seq_id);
CREATE INDEX CONCURRENTLY eventlog_8_seq_idx ON eventlog_8 USING BRIN(seq_id);
CREATE INDEX CONCURRENTLY eventlog_9_seq_idx ON eventlog_9 USING BRIN(seq_id);
CREATE INDEX CONCURRENTLY eventlog_10_seq_idx ON eventlog_10 USING BRIN(seq_id);

-- index on epoch
CREATE INDEX CONCURRENTLY eventlog_0_epoch_idx ON eventlog_0 USING BRIN(epoch);
CREATE INDEX CONCURRENTLY eventlog_1_epoch_idx ON eventlog_1 USING BRIN(epoch);
CREATE INDEX CONCURRENTLY eventlog_1_epoch_idx ON eventlog_1 USING BRIN(epoch);
CREATE INDEX CONCURRENTLY eventlog_2_epoch_idx ON eventlog_2 USING BRIN(epoch);
CREATE INDEX CONCURRENTLY eventlog_3_epoch_idx ON eventlog_3 USING BRIN(epoch);
CREATE INDEX CONCURRENTLY eventlog_4_epoch_idx ON eventlog_4 USING BRIN(epoch);
CREATE INDEX CONCURRENTLY eventlog_5_epoch_idx ON eventlog_5 USING BRIN(epoch);
CREATE INDEX CONCURRENTLY eventlog_6_epoch_idx ON eventlog_6 USING BRIN(epoch);
CREATE INDEX CONCURRENTLY eventlog_7_epoch_idx ON eventlog_7 USING BRIN(epoch);
CREATE INDEX CONCURRENTLY eventlog_8_epoch_idx ON eventlog_8 USING BRIN(epoch);
CREATE INDEX CONCURRENTLY eventlog_9_epoch_idx ON eventlog_9 USING BRIN(epoch);
CREATE INDEX CONCURRENTLY eventlog_10_epoch_idx ON eventlog_10 USING BRIN(epoch);

-- index on date
CREATE INDEX CONCURRENTLY eventlog_0_date_idx ON eventlog_0 USING BTREE (cast(dt as date));
CREATE INDEX CONCURRENTLY eventlog_1_date_idx ON eventlog_1 USING BTREE (cast(dt as date));
CREATE INDEX CONCURRENTLY eventlog_2_date_idx ON eventlog_2 USING BTREE (cast(dt as date));
CREATE INDEX CONCURRENTLY eventlog_3_date_idx ON eventlog_3 USING BTREE (cast(dt as date));
CREATE INDEX CONCURRENTLY eventlog_4_date_idx ON eventlog_4 USING BTREE (cast(dt as date));
CREATE INDEX CONCURRENTLY eventlog_5_date_idx ON eventlog_5 USING BTREE (cast(dt as date));
CREATE INDEX CONCURRENTLY eventlog_6_date_idx ON eventlog_6 USING BTREE (cast(dt as date));
CREATE INDEX CONCURRENTLY eventlog_7_date_idx ON eventlog_7 USING BTREE (cast(dt as date));
CREATE INDEX CONCURRENTLY eventlog_8_date_idx ON eventlog_8 USING BTREE (cast(dt as date));
CREATE INDEX CONCURRENTLY eventlog_9_date_idx ON eventlog_9 USING BTREE (cast(dt as date));
CREATE INDEX CONCURRENTLY eventlog_10_date_idx ON eventlog_10 USING BTREE (cast(dt as date));


--###########
--###########
############
#rawlog
--###########
--###########
--###########


DROP TABLE IF EXISTS rawlog CASCADE;


CREATE TABLE rawlog
(
    -- standard DIAS fields
	seq_id SERIAL NOT NULL
	,dt TIMESTAMP NOT NULL
    
    ,network NUMERIC NOT NULL DEFAULT 0 -- the name of the DIAS aggregation network on which the peer is running
    
	,peer NUMERIC NOT NULL
	,epoch BIGINT NOT NULL
    
    -- specific fields
    ,thread_id NUMERIC NOT NULL
    ,error_level NUMERIC NOT NULL -- 1: info, 2: warn, 3: error
    ,txt TEXT
	
);

CREATE INDEX CONCURRENTLY rawlog_seq_idx ON rawlog USING BRIN(seq_id);	
CREATE INDEX CONCURRENTLY rawlog_epoch_idx ON rawlog USING BRIN(epoch);	
CREATE INDEX CONCURRENTLY rawlog_date_idx ON rawlog USING BTREE (cast(dt as date));

--###########
--###########
############
#memlog
--###########
--###########
--###########

DROP TABLE IF EXISTS memlog CASCADE;

 
CREATE TABLE memlog
(
    -- standard DIAS fields
	seq_id SERIAL NOT NULL
	,dt TIMESTAMP NOT NULL
    
    ,network NUMERIC NOT NULL DEFAULT 0 -- the name of the DIAS aggregation network on which the peer is running
    
	,peer NUMERIC NOT NULL
	,epoch BIGINT NOT NULL
    
    -- specific fields
    ,object_group_name TEXT NOT NULL
    ,object_name TEXT NOT NULL
    ,object_size_mb FLOAT -- can be null of the object is null
);



-- index on seq_id for peers 1 to 40
CREATE INDEX CONCURRENTLY memlog_seq_idx ON memlog USING BRIN(seq_id);

--###########
--###########
############
--###########
--###########
--###########
--###########
--###########
############
--###########
--###########
--###########

--###########
--###########
############
#globalComplexCostLogger
--###########
--###########
--###########

DROP TABLE IF EXISTS globalComplexCostLogger CASCADE;

CREATE TABLE globalComplexCostLogger
(
    -- required fields
	seq_id SERIAL NOT NULL		-- auto-increment field
	,sim NUMERIC NOT NULL
	,run NUMERIC NOT NULL
	,peer NUMERIC NOT NULL
	,iteration NUMERIC NOT NULL
	,cost NUMERIC(30,15)
);


--###########
--###########
############
#GlobalCostLogger
--###########
--###########
--###########

DROP TABLE IF EXISTS GlobalCostLogger CASCADE;

CREATE TABLE GlobalCostLogger
(
    -- required fields
	seq_id SERIAL NOT NULL		-- auto-increment field
	,sim NUMERIC NOT NULL
	,run NUMERIC NOT NULL
	,peer NUMERIC NOT NULL
	,iteration NUMERIC NOT NULL
	,cost NUMERIC(30,15)
);

-- indexes
CREATE INDEX CONCURRENTLY GlobalCostLogger_seq_idx ON GlobalCostLogger USING BRIN(seq_id);	
CREATE INDEX CONCURRENTLY GlobalCostLogger_seq_sim ON GlobalCostLogger USING BRIN(sim);	
CREATE INDEX CONCURRENTLY GlobalCostLogger_seq_run ON GlobalCostLogger USING BRIN(run);	
CREATE INDEX CONCURRENTLY GlobalCostLogger_seq_peer ON GlobalCostLogger USING BRIN(peer);
CREATE INDEX CONCURRENTLY GlobalCostLogger_seq_iteration ON GlobalCostLogger USING BRIN(iteration);
CREATE INDEX CONCURRENTLY GlobalCostLogger_seq_cost ON GlobalCostLogger USING BRIN(cost);

--###########
--###########
############
#GlobalResponseVectorLogger
--###########
--###########
--###########

DROP TABLE IF EXISTS GlobalResponseVectorLogger CASCADE;

CREATE TABLE GlobalResponseVectorLogger
(
    -- required fields
	seq_id SERIAL NOT NULL		-- auto-increment field
	,sim NUMERIC NOT NULL
	,run NUMERIC NOT NULL
	,peer NUMERIC NOT NULL
	,iteration NUMERIC NOT NULL
	,globalresponse varchar
);


--###########
--###########
############
#LocalCostMultiObjectiveLogger
--###########
--###########
--###########

DROP TABLE IF EXISTS LocalCostMultiObjectiveLogger CASCADE;

CREATE TABLE LocalCostMultiObjectiveLogger
(
    -- required fields
	seq_id SERIAL NOT NULL		-- auto-increment field
	,sim NUMERIC NOT NULL
	,run NUMERIC NOT NULL
	,peer NUMERIC NOT NULL
	,iteration NUMERIC NOT NULL
	,cost NUMERIC
);

-- indexes
CREATE INDEX CONCURRENTLY LocalCostMultiObjectiveLogger_seq_idx ON LocalCostMultiObjectiveLogger USING BRIN(seq_id);	
CREATE INDEX CONCURRENTLY LocalCostMultiObjectiveLogger_seq_sim ON LocalCostMultiObjectiveLogger USING BRIN(sim);
CREATE INDEX CONCURRENTLY LocalCostMultiObjectiveLogger_seq_run ON LocalCostMultiObjectiveLogger USING BRIN(run);	
CREATE INDEX CONCURRENTLY LocalCostMultiObjectiveLogger_seq_peer ON LocalCostMultiObjectiveLogger USING BRIN(peer);
CREATE INDEX CONCURRENTLY LocalCostMultiObjectiveLogger_seq_iteration ON LocalCostMultiObjectiveLogger USING BRIN(iteration);
CREATE INDEX CONCURRENTLY LocalCostMultiObjectiveLogger_seq_cost ON LocalCostMultiObjectiveLogger USING BRIN(cost);


--###########
--###########
############
#SelectedPlanLogger
--###########
--###########
--###########

DROP TABLE IF EXISTS SelectedPlanLogger CASCADE;

CREATE TABLE SelectedPlanLogger
(
    -- required fields
	seq_id SERIAL NOT NULL		-- auto-increment field
	,sim NUMERIC NOT NULL
	,run NUMERIC NOT NULL
	,peer NUMERIC NOT NULL
	,iteration NUMERIC NOT NULL
	,planID NUMERIC
	,unfairnessWeight NUMERIC
	,localcostWeight NUMERIC
);

-- indexes
CREATE INDEX CONCURRENTLY SelectedPlanLogger_seq_idx ON SelectedPlanLogger USING BRIN(seq_id);	
CREATE INDEX CONCURRENTLY SelectedPlanLogger_seq_sim ON SelectedPlanLogger USING BRIN(sim);
CREATE INDEX CONCURRENTLY SelectedPlanLogger_seq_run ON SelectedPlanLogger USING BRIN(run);	
CREATE INDEX CONCURRENTLY SelectedPlanLogger_seq_peer ON SelectedPlanLogger USING BRIN(peer);
CREATE INDEX CONCURRENTLY SelectedPlanLogger_seq_iteration ON SelectedPlanLogger USING BRIN(iteration);

--###########
--###########
############
#TerminationLogger
--###########
--###########
--###########

DROP TABLE IF EXISTS TerminationLogger CASCADE;

CREATE TABLE TerminationLogger
(
    -- required fields
	seq_id SERIAL NOT NULL		-- auto-increment field
	,sim NUMERIC NOT NULL
	,run NUMERIC NOT NULL
	,peer NUMERIC NOT NULL
	,termination NUMERIC
);

-- indexes
CREATE INDEX CONCURRENTLY TerminationLogger_seq_idx ON TerminationLogger USING BRIN(seq_id);	
CREATE INDEX CONCURRENTLY TerminationLogger_seq_run ON TerminationLogger USING BRIN(run);	
CREATE INDEX CONCURRENTLY TerminationLogger_seq_peer ON TerminationLogger USING BRIN(peer);


--###########
--###########
############
#UnfairnessLogger
--###########
--###########
--###########

DROP TABLE IF EXISTS UnfairnessLogger CASCADE;

CREATE TABLE UnfairnessLogger
(
    -- required fields
	seq_id SERIAL NOT NULL		-- auto-increment field
	,sim NUMERIC NOT NULL
	,run NUMERIC NOT NULL
	,peer NUMERIC NOT NULL
	,iteration NUMERIC NOT NULL
	,unfairness NUMERIC
);

-- indexes
CREATE INDEX CONCURRENTLY UnfairnessLogger_seq_idx ON UnfairnessLogger USING BRIN(seq_id);	
CREATE INDEX CONCURRENTLY UnfairnessLogger_seq_run ON UnfairnessLogger USING BRIN(run);	
CREATE INDEX CONCURRENTLY UnfairnessLogger_seq_peer ON UnfairnessLogger USING BRIN(peer);
CREATE INDEX CONCURRENTLY UnfairnessLogger_seq_iteration ON UnfairnessLogger USING BRIN(iteration);	


--###########
--###########
############
#msgs
--###########
--###########
--###########

DROP TABLE IF EXISTS msgs CASCADE;

 
CREATE TABLE msgs
(
    -- standard DIAS fields
	seq_id SERIAL NOT NULL
	,dt TIMESTAMP NOT NULL
    
    ,network INTEGER NOT NULL DEFAULT 0 -- the name of the DIAS aggregation network on which the peer is running
    
	,peer INTEGER NOT NULL
	,epoch BIGINT NOT NULL
    
    -- specific fields
    ,dt_send TIMESTAMP
    ,dt_rec TIMESTAMP NOT NULL
    ,dir TEXT NOT NULL
    ,finger_from TEXT 
    ,finger_to TEXT 
    ,mtype TEXT NOT NULL
    ,msg TEXT
    
    -- add 2019-04-04
    ,msg_uuid TEXT NOT NULL	-- unique ID (UUID4) of the message
    
    
);

-- indexes
CREATE INDEX CONCURRENTLY msgs_seq_idx ON msgs USING BRIN(seq_id);	
CREATE INDEX CONCURRENTLY msgs_epoch_idx ON msgs USING BRIN(epoch);	
CREATE INDEX CONCURRENTLY msgs_date_idx ON msgs USING BTREE (cast(dt as date));
CREATE INDEX CONCURRENTLY msgs_msg_uuid ON msgs USING BTREE (msg_uuid);

\c epos;
