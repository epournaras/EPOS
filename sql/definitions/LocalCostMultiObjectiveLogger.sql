DROP TABLE IF EXISTS LocalCostMultiObjectiveLogger CASCADE;

CREATE TABLE LocalCostMultiObjectiveLogger
(
    -- required fields
	seq_id SERIAL NOT NULL		-- auto-increment field
	,run INTEGER NOT NULL
	,peer INTEGER NOT NULL
	,iteration INTEGER NOT NULL
	,cost INTEGER NOT NULL
);

