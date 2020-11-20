DROP TABLE IF EXISTS globalComplexCostLogger CASCADE;

CREATE TABLE globalComplexCostLogger
(
    -- required fields
	seq_id SERIAL NOT NULL		-- auto-increment field
	,run INTEGER NOT NULL
	,peer INTEGER NOT NULL
	,iteration INTEGER NOT NULL
	,cost INTEGER NOT NULL
);

