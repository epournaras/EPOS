DROP TABLE IF EXISTS customlog CASCADE;

CREATE TABLE customlog
(
    -- required fields
	seq_id SERIAL NOT NULL		-- auto-increment field
	,dt TIMESTAMP NOT NULL
    
    ,run INTEGER NOT NULL 
	,iteration INTEGER NOT NULL
	,dim_0 INTEGER NOT NULL
	,dim_1 INTEGER NOT NULL
	--,dim_... INTEGER NOT NULL

	
);

