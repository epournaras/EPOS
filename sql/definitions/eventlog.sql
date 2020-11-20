DROP TABLE IF EXISTS eventlog CASCADE;

 --DROP TABLE eventlog_default;
CREATE TABLE eventlog
(
    -- standard DIAS fields
	seq_id SERIAL NOT NULL
	,dt TIMESTAMP NOT NULL
    ,current_time_millis BIGINT NOT NULL -- timestamp in milli-seconds set by the peer
    
    ,network INTEGER NOT NULL DEFAULT 0 -- the name of the DIAS aggregation network on which the peer is running
    
	,peer INTEGER NOT NULL
	,epoch BIGINT NOT NULL
    ,peer_event_counter BIGINT NOT NULL -- internal counter of the events inside the peer
    
    -- specific fields
    ,thread_id INTEGER NOT NULL
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
