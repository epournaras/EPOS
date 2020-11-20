DROP TABLE IF EXISTS GlobalCostLogger CASCADE;

CREATE TABLE GlobalCostLogger
(
    -- required fields
	seq_id SERIAL NOT NULL		-- auto-increment field
	,run INTEGER NOT NULL
	,peer INTEGER NOT NULL
	,iteration INTEGER NOT NULL
	,cost INTEGER NOT NULL
);

