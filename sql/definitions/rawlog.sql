DROP TABLE IF EXISTS rawlog CASCADE;


CREATE TABLE rawlog
(
    -- standard DIAS fields
	seq_id SERIAL NOT NULL
	,dt TIMESTAMP NOT NULL
    
    ,network INTEGER NOT NULL DEFAULT 0 -- the name of the DIAS aggregation network on which the peer is running
    
	,peer INTEGER NOT NULL
	,epoch BIGINT NOT NULL
    
    -- specific fields
    ,thread_id INTEGER NOT NULL
    ,error_level INTEGER NOT NULL -- 1: info, 2: warn, 3: error
    ,txt TEXT
	
);

CREATE INDEX CONCURRENTLY rawlog_seq_idx ON rawlog USING BRIN(seq_id);	
CREATE INDEX CONCURRENTLY rawlog_epoch_idx ON rawlog USING BRIN(epoch);	
CREATE INDEX CONCURRENTLY rawlog_date_idx ON rawlog USING BTREE (cast(dt as date));

