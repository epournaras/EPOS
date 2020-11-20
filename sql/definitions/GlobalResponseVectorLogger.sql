DROP TABLE IF EXISTS GlobalResponseVectorLogger CASCADE;

CREATE TABLE GlobalResponseVectorLogger
(
    -- required fields
	seq_id SERIAL NOT NULL		-- auto-increment field
	,run INTEGER NOT NULL
	,peer INTEGER NOT NULL
	,iteration INTEGER NOT NULL
	,globalresponse varchar NOT NULL
);

