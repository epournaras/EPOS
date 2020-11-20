DROP TABLE IF EXISTS memlog CASCADE;

 
CREATE TABLE memlog
(
    -- standard DIAS fields
	seq_id SERIAL NOT NULL
	,dt TIMESTAMP NOT NULL
    
    ,network INTEGER NOT NULL DEFAULT 0 -- the name of the DIAS aggregation network on which the peer is running
    
	,peer INTEGER NOT NULL
	,epoch BIGINT NOT NULL
    
    -- specific fields
    ,object_group_name TEXT NOT NULL
    ,object_name TEXT NOT NULL
    ,object_size_mb FLOAT -- can be null of the object is null
);



-- index on seq_id for peers 1 to 40
CREATE INDEX CONCURRENTLY memlog_seq_idx ON memlog USING BRIN(seq_id);
