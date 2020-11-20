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